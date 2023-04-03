package com.bumptech.glide.load.resource.gif;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Preconditions;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class GifFrameResourceDecoderTest {
  private GifDecoder gifDecoder;
  private GifFrameResourceDecoder resourceDecoder;
  private Options options;

  @Before
  public void setUp() {
    gifDecoder = mock(GifDecoder.class);
    resourceDecoder = new GifFrameResourceDecoder(mock(BitmapPool.class));
    options = new Options();
  }

  @Test
  public void testReturnsFrameFromGifDecoder() throws IOException {
    Bitmap expected = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_4444);
    when(gifDecoder.getNextFrame()).thenReturn(expected);

    assertEquals(
        expected,
        Preconditions.checkNotNull(resourceDecoder.decode(gifDecoder, 100, 100, options)).get());
  }

  @Test
  public void testReturnsNullIfGifDecoderReturnsNullFrame() {
    when(gifDecoder.getNextFrame()).thenReturn(null);

    assertNull(resourceDecoder.decode(gifDecoder, 100, 100, options));
  }
}
