package com.bumptech.glide.load.resource.gif;

import android.graphics.Bitmap;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.tests.Util;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GifFrameResourceDecoderTest {
    private GifDecoder gifDecoder;
    private GifFrameResourceDecoder resourceDecoder;

    @Before
    public void setUp() {
        gifDecoder = mock(GifDecoder.class);
        resourceDecoder = new GifFrameResourceDecoder(mock(BitmapPool.class));
    }

    @Test
    public void testReturnsValidId() {
        Util.assertClassHasValidId(GifFrameResourceDecoder.class, resourceDecoder.getId());
    }

    @Test
    public void testReturnsFrameFromGifDecoder() throws IOException {
        Bitmap expected = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_4444);
        when(gifDecoder.getNextFrame()).thenReturn(expected);

        assertEquals(expected, resourceDecoder.decode(gifDecoder, 100, 100).get());
    }
}
