package com.bumptech.glide.load.resource.bitmap;

import static com.bumptech.glide.tests.Util.mockResource;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
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
  public void testEncoderObeysNonNullCompressFormat() throws IOException {
    Bitmap.CompressFormat format = Bitmap.CompressFormat.WEBP;
    harness.setFormat(format);

    String fakeBytes = harness.encode();

    assertContains(fakeBytes, format.toString());
  }

  @Test
  public void testEncoderEncodesJpegWithNullFormatAndBitmapWithoutAlpha() throws IOException {
    harness.setFormat(null);
    harness.bitmap.setHasAlpha(false);

    String fakeBytes = harness.encode();

    assertContains(fakeBytes, Bitmap.CompressFormat.JPEG.toString());
  }

  @Test
  public void testEncoderEncodesPngWithNullFormatAndBitmapWithAlpha() throws IOException {
    harness.setFormat(null);
    harness.bitmap.setHasAlpha(true);

    String fakeBytes = harness.encode();

    assertContains(fakeBytes, Bitmap.CompressFormat.PNG.toString());
  }

  @Test
  public void testReturnsTrueFromWrite() {
    BitmapEncoder encoder = new BitmapEncoder(harness.arrayPool);
    assertTrue(encoder.encode(harness.resource, harness.file, harness.options));
  }

  @Test
  public void testEncodeStrategy_alwaysReturnsTransformed() {
    BitmapEncoder encoder = new BitmapEncoder(harness.arrayPool);
    assertEquals(EncodeStrategy.TRANSFORMED, encoder.getEncodeStrategy(harness.options));
  }

  private static void assertContains(String string, String expected) {
    assertThat(string).contains(expected);
  }

  private static class EncoderHarness {
    final Resource<Bitmap> resource = mockResource();
    final Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    final Options options = new Options();
    final File file = new File(RuntimeEnvironment.application.getCacheDir(), "test");
    final ArrayPool arrayPool = new LruArrayPool();

    EncoderHarness() {
      when(resource.get()).thenReturn(bitmap);
    }

    void setQuality(int quality) {
      options.set(BitmapEncoder.COMPRESSION_QUALITY, quality);
    }

    void setFormat(Bitmap.CompressFormat format) {
      options.set(BitmapEncoder.COMPRESSION_FORMAT, format);
    }

    String encode() throws IOException {
      BitmapEncoder encoder = new BitmapEncoder(arrayPool);
      encoder.encode(resource, file, options);
      byte[] data = ByteBufferUtil.toBytes(ByteBufferUtil.fromFile(file));
      return new String(data, "UTF-8");
    }

    void tearDown() {
      // GC before delete() to release files on Windows (https://stackoverflow.com/a/4213208/253468)
      System.gc();
      if (file.exists() && !file.delete()) {
        throw new IllegalStateException("Failed to delete: " + file);
      }
    }
  }
}
