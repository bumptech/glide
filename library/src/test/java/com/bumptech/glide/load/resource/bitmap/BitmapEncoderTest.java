package com.bumptech.glide.load.resource.bitmap;

import static com.bumptech.glide.tests.Util.mockResource;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.ByteBufferUtil;
import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class BitmapEncoderTest {
  private EncoderHarness harness;

  @Before
  public void setUp() {
    harness = new EncoderHarness();
  }

  @After
  public void tearDown() {
    harness.tearDown();
  }

  @Test
  public void testBitmapIsEncoded() throws IOException {
    String fakeBytes = harness.encode();

    assertContains(fakeBytes, Shadows.shadowOf(harness.bitmap).getDescription());
  }

  @Test
  public void testBitmapIsEncodedWithGivenQuality() throws IOException {
    int quality = 7;
    harness.setQuality(quality);

    String fakeBytes = harness.encode();

    assertContains(fakeBytes, String.valueOf(quality));
  }

  @Test
  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  public void testEncoderObeysNonNullCompressFormat() throws IOException {
    Bitmap.CompressFormat format = Bitmap.CompressFormat.WEBP;
    harness.setFormat(format);

    String fakeBytes = harness.encode();

    assertContains(fakeBytes, format.toString());
  }

  @Test
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  public void testEncoderEncodesJpegWithNullFormatAndBitmapWithoutAlpha() throws IOException {
    harness.setFormat(null);
    harness.bitmap.setHasAlpha(false);

    String fakeBytes = harness.encode();

    assertContains(fakeBytes, Bitmap.CompressFormat.JPEG.toString());
  }

  @Test
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  public void testEncoderEncodesPngWithNullFormatAndBitmapWithAlpha() throws IOException {
    harness.setFormat(null);
    harness.bitmap.setHasAlpha(true);

    String fakeBytes = harness.encode();

    assertContains(fakeBytes, Bitmap.CompressFormat.PNG.toString());
  }

  @Test
  public void testReturnsTrueFromWrite() {
    BitmapEncoder encoder = new BitmapEncoder();
    assertTrue(encoder.encode(harness.resource, harness.file, harness.options));
  }

  @Test
  public void testEncodeStrategy_alwaysReturnsTransformed() {
    BitmapEncoder encoder = new BitmapEncoder();
    assertEquals(EncodeStrategy.TRANSFORMED, encoder.getEncodeStrategy(harness.options));
  }

  private static void assertContains(String string, String expected) {
    assertThat(string).contains(expected);
  }

  private static class EncoderHarness {
    Resource<Bitmap> resource = mockResource();
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    Options options = new Options();
    File file = new File(RuntimeEnvironment.application.getCacheDir(), "test");

    public EncoderHarness() {
      when(resource.get()).thenReturn(bitmap);
    }

    public void setQuality(int quality) {
      options.set(BitmapEncoder.COMPRESSION_QUALITY, quality);
    }

    public void setFormat(Bitmap.CompressFormat format) {
      options.set(BitmapEncoder.COMPRESSION_FORMAT, format);
    }

    public String encode() throws IOException {
      BitmapEncoder encoder = new BitmapEncoder();
      encoder.encode(resource, file, options);
      byte[] data = ByteBufferUtil.toBytes(ByteBufferUtil.fromFile(file));
      return new String(data);
    }

    public void tearDown() {
      file.delete();
    }
  }
}
