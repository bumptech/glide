package com.bumptech.glide.load.resource.gifbitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.resource.gif.GifData;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GifBitmapWrapperTest {

    @Test
    public void testReturnsBitmapResourceIfHasBitmapResource() {
        Resource<Bitmap> bitmapResource = mock(Resource.class);
        GifBitmapWrapper wrapper = new GifBitmapWrapper(bitmapResource, null);
        assertEquals(bitmapResource, wrapper.getBitmapResource());
    }

    @Test
    public void testReturnsGifResourceIfHasGifResource() {
        Resource<GifData> gifDataResource = mock(Resource.class);
        GifBitmapWrapper wrapper = new GifBitmapWrapper(null, gifDataResource);
        assertEquals(gifDataResource, wrapper.getGifResource());
    }

    @Test
    public void testReturnsBitmapSizeIfHasBitmap() {
        final int size = 1234;
        Resource<Bitmap> bitmapResource = mock(Resource.class);
        when(bitmapResource.getSize()).thenReturn(size);
        GifBitmapWrapper wrapper = new GifBitmapWrapper(bitmapResource, null);

        assertEquals(size, wrapper.getSize());
    }

    @Test
    public void testReturnsGifSizeIfHasGif() {
        final int size = 48523;
        Resource<GifData> gifDataResource = mock(Resource.class);
        when(gifDataResource.getSize()).thenReturn(size);
        GifBitmapWrapper wrapper = new GifBitmapWrapper(null, gifDataResource);

        assertEquals(size, wrapper.getSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfGivenBothBitmapAndGif() {
        Resource<Bitmap> bitmapResource = mock(Resource.class);
        Resource<GifData> gifDataResource = mock(Resource.class);
        new GifBitmapWrapper(bitmapResource, gifDataResource);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfGivenNoResources() {
        new GifBitmapWrapper(null, null);
    }
}