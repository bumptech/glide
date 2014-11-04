package com.bumptech.glide.load.resource.gifbitmap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.gif.GifDrawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GifBitmapWrapperResourceTest {
    private GifBitmapWrapper data;
    private GifBitmapWrapperResource resource;

    @Before
    public void setUp() {
        data = mock(GifBitmapWrapper.class);
        resource = new GifBitmapWrapperResource(data);
    }

    @Test
    public void testReturnsDataSize() {
        int size = 332;
        when(data.getSize()).thenReturn(size);

        assertEquals(size, resource.getSize());
    }

    @Test
    public void testReturnsGivenData() {
        assertEquals(data, resource.get());
    }

    @Test
    public void testRecyclesBitmapResource() {
        Resource<Bitmap> bitmapResource = mock(Resource.class);
        when(data.getBitmapResource()).thenReturn(bitmapResource);

        resource.recycle();

        verify(bitmapResource).recycle();
    }

    @Test
    public void testRecyclesGifResource() {
        Resource<GifDrawable> gifDataResource = mock(Resource.class);
        when(data.getGifResource()).thenReturn(gifDataResource);

        resource.recycle();

        verify(gifDataResource).recycle();
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsIfGivenWrapperIsNull() {
        new GifBitmapWrapperResource(null);
    }
}