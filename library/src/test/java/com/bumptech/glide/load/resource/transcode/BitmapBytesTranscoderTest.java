package com.bumptech.glide.load.resource.transcode;

import static com.bumptech.glide.tests.Util.mockResource;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import com.bumptech.glide.load.engine.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class BitmapBytesTranscoderTest {
  private BitmapBytesTranscoderHarness harness;

  @Before()
  public void setUp() {
    harness = new BitmapBytesTranscoderHarness();
  }

  @Test
  public void testReturnsBytesOfGivenBitmap() {
    String transcodedDescription = harness.getTranscodedDescription();
    assertThat(transcodedDescription).startsWith(harness.description);
  }

  @Test
  public void testUsesGivenQuality() {
    harness.quality = 66;
    String transcodedDescription = harness.getTranscodedDescription();
    assertThat(transcodedDescription).contains(String.valueOf(harness.quality));
  }

  @Test
  public void testUsesGivenFormat() {
    for (Bitmap.CompressFormat format : Bitmap.CompressFormat.values()) {
      harness.compressFormat = format;
      String transcodedDescription = harness.getTranscodedDescription();
      assertThat(transcodedDescription).contains(format.name());
    }
  }

  @Test
  public void testBitmapResourceIsRecycled() {
    harness.getTranscodedDescription();

    verify(harness.bitmapResource).recycle();
  }

  private static class BitmapBytesTranscoderHarness {
    Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
    int quality = 100;
    final String description = "TestDescription";
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ALPHA_8);
    Resource<Bitmap> bitmapResource = mockResource();

    public BitmapBytesTranscoderHarness() {
      when(bitmapResource.get()).thenReturn(bitmap);
      Shadows.shadowOf(bitmap).setDescription(description);
    }

    public String getTranscodedDescription() {
      BitmapBytesTranscoder transcoder = new BitmapBytesTranscoder(compressFormat, quality);
      Resource<byte[]> bytesResource = transcoder.transcode(bitmapResource);

      return new String(bytesResource.get());
    }
  }
}
