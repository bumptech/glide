package com.bumptech.glide.load.resource.transcode;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.bumptech.glide.tests.Util.mockResource;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class BitmapDrawableTranscoderTest {
  private BitmapDrawableTranscoder transcoder;

  @Before
  public void setUp() {
    transcoder =
        new BitmapDrawableTranscoder(ApplicationProvider.getApplicationContext().getResources());
  }

  @Test
  public void testReturnsBitmapDrawableResourceContainingGivenBitmap() {
    Bitmap expected = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    Resource<Bitmap> resource = mockResource();
    when(resource.get()).thenReturn(expected);

    Resource<BitmapDrawable> transcoded = transcoder.transcode(resource, new Options());

    assertEquals(expected, transcoded.get().getBitmap());
  }
}
