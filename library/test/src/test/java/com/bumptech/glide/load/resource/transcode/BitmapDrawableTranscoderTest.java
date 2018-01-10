package com.bumptech.glide.load.resource.transcode;

import static com.bumptech.glide.tests.Util.mockResource;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class BitmapDrawableTranscoderTest {
  private BitmapDrawableTranscoder transcoder;

  @Before
  public void setUp() {
    transcoder = new BitmapDrawableTranscoder(RuntimeEnvironment.application.getResources());
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
