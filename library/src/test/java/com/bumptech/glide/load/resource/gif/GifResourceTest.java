package com.bumptech.glide.load.resource.gif;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GifResourceTest {
    private GifDecoder decoder;
    private GifDrawable gifDrawable;
    private GifResource resource;

    @Before
    public void setUp() {
        decoder = mock(GifDecoder.class);
        gifDrawable = mock(GifDrawable.class);
        resource = new GifResource(decoder, gifDrawable);
    }

    @Test
    public void testSizeReturnsGifDecoderGifByteSize() {
        int size = 1234;
        when(decoder.getGifByteSize()).thenReturn(size);

        assertEquals(size, resource.getSize());
    }

    @Test
    public void testReturnsNonNullDrawable() {
        assertNotNull(resource.get());
    }

    @Test
    public void testStopsGifDrawableOnRecycle() {
        resource.recycle();

        verify(gifDrawable).stop();
    }

    @Test
    public void testRecyclesGifDrawableOnRecycle() {
        resource.recycle();

        verify(gifDrawable).recycle();
    }
}
