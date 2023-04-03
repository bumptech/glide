package com.bumptech.glide.load.resource.transcode;

import static com.bumptech.glide.tests.Util.mockResource;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.Preconditions;
import java.io.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class BitmapBytesTranscoderTest {
  private BitmapBytesTranscoderHarness harness;

  @Before()
  public void setUp() {
    harness = new BitmapBytesTranscoderHarness();
  }

  @Test
  public void testReturnsBytesOfGivenBitmap() {
    assertThat(harness.getTranscodeResult()).isEqualTo(harness.getExpectedData());
  }

  @Test
  public void testUsesGivenQuality() {
    harness.quality = 66;
    assertThat(harness.getTranscodeResult()).isEqualTo(harness.getExpectedData());
  }

  @Test
  public void testUsesGivenFormat() {
    for (Bitmap.CompressFormat format : Bitmap.CompressFormat.values()) {
      harness.compressFormat = format;
      assertThat(harness.getTranscodeResult()).isEqualTo(harness.getExpectedData());
    }
  }

  @Test
  public void testBitmapResourceIsRecycled() {
    harness.getTranscodeResult();

    verify(harness.bitmapResource).recycle();
  }

  private static class BitmapBytesTranscoderHarness {
    Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
    int quality = 100;
    final Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ALPHA_8);
    final Resource<Bitmap> bitmapResource = mockResource();
    final Options options = new Options();

    BitmapBytesTranscoderHarness() {
      when(bitmapResource.get()).thenReturn(bitmap);
    }

    byte[] getTranscodeResult() {
      BitmapBytesTranscoder transcoder = new BitmapBytesTranscoder(compressFormat, quality);
      Resource<byte[]> bytesResource =
          Preconditions.checkNotNull(transcoder.transcode(bitmapResource, options));

      return bytesResource.get();
    }

    byte[] getExpectedData() {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      bitmap.compress(compressFormat, quality, os);
      return os.toByteArray();
    }
  }
}
