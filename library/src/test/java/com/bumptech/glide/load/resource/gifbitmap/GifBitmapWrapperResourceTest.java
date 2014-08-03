package com.bumptech.glide.load.resource.gifbitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}