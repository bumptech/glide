package com.bumptech.glide.load.resource.gif;

import android.graphics.Bitmap;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.resource.gif.decoder.GifDecoder;
import com.bumptech.glide.tests.Util;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GifFrameResourceDecoderTest {
    private GifDecoder gifDecoder;
    private GifFrameResourceDecoder resourceDecoder;

    @Before
    public void setUp() {
        gifDecoder = mock(GifDecoder.class);
        resourceDecoder = new GifFrameResourceDecoder();
    }

    @Test
    public void testReturnsValidId() {
        Util.assertClassHasValidId(GifFrameResourceDecoder.class, resourceDecoder.getId());
    }

    @Test
    public void testReturnsFrameFromGifDecoder() throws IOException {
        Resource<Bitmap> resource = mock(Resource.class);
        when(gifDecoder.getNextFrame()).thenReturn(resource);

        assertEquals(resource, resourceDecoder.decode(gifDecoder, 100, 100));
    }
}
