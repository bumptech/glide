package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.collect.Range.closed;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.ByteArrayPool;
import com.bumptech.glide.load.resource.bitmap.DownsamplerTest.AllocationSizeBitmap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowBitmap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 19, shadows = AllocationSizeBitmap.class)
public class DownsamplerTest {
  @Mock private BitmapPool bitmapPool;
  @Mock private ByteArrayPool byteArrayPool;
  private Downsampler downsampler;
  private Options options;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    options = new Options();
    DisplayMetrics displayMetrics =
        RuntimeEnvironment.application.getResources().getDisplayMetrics();
    when(byteArrayPool.get(anyInt()))
        .thenReturn(new byte[ByteArrayPool.STANDARD_BUFFER_SIZE_BYTES]);
    downsampler = new Downsampler(displayMetrics, bitmapPool, byteArrayPool);
  }

  @Test
  public void testAlwaysArgb8888() throws IOException {
    Bitmap rgb565 = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
    InputStream stream = compressBitmap(rgb565, Bitmap.CompressFormat.JPEG);

    options.set(Downsampler.DECODE_FORMAT, DecodeFormat.PREFER_ARGB_8888);
    Resource<Bitmap> result = downsampler.decode(stream, 100, 100, options);
    assertEquals(Bitmap.Config.ARGB_8888, result.get().getConfig());
  }

  @Test
  public void testPreferRgb565() throws IOException {
    Bitmap rgb565 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    InputStream stream = compressBitmap(rgb565, Bitmap.CompressFormat.JPEG);

    options.set(Downsampler.DECODE_FORMAT, DecodeFormat.PREFER_RGB_565);
    Resource<Bitmap> result = downsampler.decode(stream, 100, 100, options);
    assertEquals(Bitmap.Config.RGB_565, result.get().getConfig());
  }

  @Test
  public void testCalculateScaling_withInvalidSourceSizes_doesNotCrash() {
    runScaleTest(0, 0, 100, 100, DownsampleStrategy.AT_MOST, 0, 0);
    runScaleTest(-1, -1, 100, 100, DownsampleStrategy.AT_MOST, -1, -1);
    runScaleTest(0, 0, 100, 100, DownsampleStrategy.AT_LEAST, 0, 0);
    runScaleTest(-1, -1, 100, 100, DownsampleStrategy.CENTER_OUTSIDE, -1, -1);
  }

  @Test
  public void testCalculateScaling_withAtMost() {
    DownsampleStrategy strategy = DownsampleStrategy.AT_MOST;
    runScaleTest(100, 100, 100, 100, strategy, 100, 100);
    runScaleTest(200, 200, 100, 100, strategy, 100, 100);
    runScaleTest(400, 400, 100, 100, strategy, 100, 100);
    runScaleTest(300, 300, 100, 100, strategy, 75, 75);
    runScaleTest(799, 100, 100, 100, strategy, 100, 13);
    runScaleTest(800, 100, 100, 100, strategy, 100, 13);
    runScaleTest(801, 100, 100, 100, strategy, 50, 6);
    runScaleTest(100, 800, 100, 100, strategy, 13, 100);
    runScaleTest(87, 78, 100, 100, strategy, 87, 78);
  }

  @Test
  public void testCalculateScaling_withAtLeast() {
    DownsampleStrategy strategy = DownsampleStrategy.AT_LEAST;
    runScaleTest(100, 100, 100, 100, strategy, 100, 100);
    runScaleTest(200, 200, 100, 100, strategy, 100, 100);
    runScaleTest(400, 400, 100, 100, strategy, 100, 100);
    runScaleTest(300, 300, 100, 100, strategy, 150, 150);
    runScaleTest(799, 100, 100, 100, strategy, 799, 100);
    runScaleTest(800, 100, 100, 100, strategy, 800, 100);
    runScaleTest(801, 100, 100, 100, strategy, 801, 100);
    runScaleTest(100, 800, 100, 100, strategy, 100, 800);
    runScaleTest(87, 78, 100, 100, strategy, 87, 78);
  }

  @Test
  public void testCalculateScaling_withCenterInside() {
    DownsampleStrategy strategy = DownsampleStrategy.FIT_CENTER;
    runScaleTest(100, 100, 100, 100, strategy, 100, 100);
    runScaleTest(200, 200, 100, 100, strategy, 100, 100);
    runScaleTest(400, 400, 100, 100, strategy, 100, 100);
    runScaleTest(300, 300, 100, 100, strategy, 100, 100);
    runScaleTest(799, 100, 100, 100, strategy, 100, 13);
    runScaleTest(800, 100, 100, 100, strategy, 100, 13);
    runScaleTest(801, 100, 100, 100, strategy, 100, 13);
    runScaleTest(100, 800, 100, 100, strategy, 13, 100);
    runScaleTest(87, 78, 100, 100, strategy, 100, 90);
  }

  @Test
  public void testCalculateScaling_withCenterOutside() {
    DownsampleStrategy strategy = DownsampleStrategy.CENTER_OUTSIDE;
    runScaleTest(100, 100, 100, 100, strategy, 100, 100);
    runScaleTest(200, 200, 100, 100, strategy, 100, 100);
    runScaleTest(400, 400, 100, 100, strategy, 100, 100);
    runScaleTest(300, 300, 100, 100, strategy, 100, 100);
    runScaleTest(799, 100, 100, 100, strategy, 799, 100);
    runScaleTest(800, 100, 100, 100, strategy, 800, 100);
    runScaleTest(801, 100, 100, 100, strategy, 801, 100);
    runScaleTest(100, 800, 100, 100, strategy, 100, 800);
    runScaleTest(87, 78, 100, 100, strategy, 112, 100);
  }

  @Test
  public void testCalculateScaling_withNone() {
    DownsampleStrategy strategy = DownsampleStrategy.NONE;
    runScaleTest(100, 100, 100, 100, strategy, 100, 100);
    runScaleTest(200, 200, 100, 100, strategy, 200, 200);
    runScaleTest(400, 400, 100, 100, strategy, 400, 400);
    runScaleTest(300, 300, 100, 100, strategy, 300, 300);
    runScaleTest(799, 100, 100, 100, strategy, 799, 100);
    runScaleTest(800, 100, 100, 100, strategy, 800, 100);
    runScaleTest(801, 100, 100, 100, strategy, 801, 100);
    runScaleTest(100, 800, 100, 100, strategy, 100, 800);
    runScaleTest(87, 78, 100, 100, strategy, 87, 78);
  }

  private static void runScaleTest(int sourceWidth, int sourceHeight, int targetWidth,
      int targetHeight, DownsampleStrategy strategy, int expectedWidth, int expectedHeight) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    Downsampler.calculateScaling(strategy, 0, sourceWidth, sourceHeight, targetWidth, targetHeight,
        options);
    assertSize(sourceWidth, sourceHeight, expectedWidth, expectedHeight, options);
  }

  private static void assertSize(int sourceWidth, int sourceHeight, int expectedWidth,
      int expectedHeight, BitmapFactory.Options options) {
    float sampleSize = Math.max(1, options.inSampleSize);
    int downsampledWidth = (int) ((sourceWidth / sampleSize) + 0.5f);
    int downsampledHeight = (int) ((sourceHeight / sampleSize) + 0.5f);

    float scaleFactor = options.inScaled && options.inTargetDensity > 0 && options.inDensity > 0
        ? options.inTargetDensity / (float) options.inDensity : 1f;
    int scaledWidth = (int) Math.ceil(downsampledWidth * scaleFactor);
    int scaledHeight = (int) Math.ceil(downsampledHeight * scaleFactor);

    assertThat(scaledWidth).isIn(closed(expectedWidth, expectedWidth + 1));
    assertThat(scaledHeight).isIn(closed(expectedHeight, expectedHeight + 1));
  }

  private InputStream compressBitmap(Bitmap bitmap, Bitmap.CompressFormat compressFormat)
      throws FileNotFoundException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    bitmap.compress(compressFormat, 100, os);
    return new ByteArrayInputStream(os.toByteArray());
  }

  // Robolectric doesn't implement getAllocationByteCount correctly.
  @Implements(Bitmap.class)
  public static class AllocationSizeBitmap extends ShadowBitmap {

    @Implementation
    public int getAllocationByteCount() {
      return getWidth() * getHeight() * (getConfig() == Bitmap.Config.ARGB_8888 ? 4 : 2);
    }
  }
}
