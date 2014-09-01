package com.bumptech.glide.load.resource.gifbitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.InputStream;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GifBitmapWrapperResourceDecoderTest {
    private ResourceDecoder<ImageVideoWrapper, Bitmap> bitmapDecoder;
    private ResourceDecoder<InputStream, GifDrawable> gifDecoder;
    private GifBitmapWrapperResourceDecoder decoder;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        bitmapDecoder = mock(ResourceDecoder.class);
        gifDecoder = mock(ResourceDecoder.class);
        decoder = new GifBitmapWrapperResourceDecoder(bitmapDecoder, gifDecoder);
    }

    @Test
    public void testHasValidId() {
        String bitmapId = "bitmapId";
        when(bitmapDecoder.getId()).thenReturn(bitmapId);
        String gifId = "gifId";
        when(gifDecoder.getId()).thenReturn(gifId);

        String id = decoder.getId();
        assertTrue(id.contains(bitmapId));
        assertTrue(id.contains(gifId));
    }
}
