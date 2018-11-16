package com.bumptech.glide.load.resource.bitmap;

import static com.bumptech.glide.tests.Util.mockResource;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
@Ignore
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
    Bitmap bitmap = harness.encode();
    // TODO(b/119624843)
    assertThat(bitmap.getWidth()).isEqualTo(100);
    assertThat(bitmap.getHeight()).isEqualTo(100);
  }

  @Test
  public void testBitmapIsEncodedWithGivenQuality() throws IOException {
    int quality = 7;
    harness.setQuality(quality);

    Bitmap bitmap = harness.encode();
    // TODO(b/119624843)
    assertThat(bitmap.getWidth()).isEqualTo(100);
    assertThat(bitmap.getHeight()).isEqualTo(100);
  }

  @Test
  public void testEncoderObeysNonNullCompressFormat() throws IOException {
    Bitmap.CompressFormat format = Bitmap.CompressFormat.WEBP;
    harness.setFormat(format);

    Bitmap bitmap = harness.encode();
    // TODO(b/119624843)
    assertThat(bitmap.getWidth()).isEqualTo(100);
    assertThat(bitmap.getHeight()).isEqualTo(100);
  }

  @Test
  public void testEncoderEncodesJpegWithNullFormatAndBitmapWithoutAlpha() throws IOException {
    harness.setFormat(null);
    harness.bitmap.setHasAlpha(false);

    Bitmap bitmap = harness.encode();
    // TODO(b/119624843)
    assertThat(bitmap.getWidth()).isEqualTo(100);
    assertThat(bitmap.getHeight()).isEqualTo(100);
  }

  @Test
  public void testEncoderEncodesPngWithNullFormatAndBitmapWithAlpha() throws IOException {
    harness.setFormat(null);
    harness.bitmap.setHasAlpha(true);

    Bitmap bitmap = harness.encode();
    // TODO(b/119624843)
    assertThat(bitmap.getWidth()).isEqualTo(100);
    assertThat(bitmap.getHeight()).isEqualTo(100);
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

    Bitmap encode() throws IOException {
      BitmapEncoder encoder = new BitmapEncoder(arrayPool);
      encoder.encode(resource, file, options);
      byte[] data = ByteBufferUtil.toBytes(ByteBufferUtil.fromFile(file));
      return BitmapFactory.decodeByteArray(data, 0, data.length);
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
