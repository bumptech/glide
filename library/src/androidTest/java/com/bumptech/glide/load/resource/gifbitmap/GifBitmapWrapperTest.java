package com.bumptech.glide.load.resource.gifbitmap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.gif.GifDrawable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GifBitmapWrapperTest {

    @Test
    public void testReturnsBitmapResourceIfHasBitmapResource() {
        Resource<Bitmap> bitmapResource = mock(Resource.class);
        GifBitmapWrapper wrapper = new GifBitmapWrapper(bitmapResource, null);
        assertEquals(bitmapResource, wrapper.getBitmapResource());
    }

    @Test
    public void testReturnsGifResourceIfHasGifResource() {
        Resource<GifDrawable> gifDrawableResource = mock(Resource.class);
        GifBitmapWrapper wrapper = new GifBitmapWrapper(null, gifDrawableResource);
        assertEquals(gifDrawableResource, wrapper.getGifResource());
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
        Resource<GifDrawable> gifDrawableResource = mock(Resource.class);
        when(gifDrawableResource.getSize()).thenReturn(size);
        GifBitmapWrapper wrapper = new GifBitmapWrapper(null, gifDrawableResource);

        assertEquals(size, wrapper.getSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfGivenBothBitmapAndGif() {
        Resource<Bitmap> bitmapResource = mock(Resource.class);
        Resource<GifDrawable> gifDrawableResource = mock(Resource.class);
        new GifBitmapWrapper(bitmapResource, gifDrawableResource);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfGivenNoResources() {
        new GifBitmapWrapper(null, null);
    }
}