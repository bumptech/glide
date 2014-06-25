package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.load.engine.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GifDrawableResourceTest {
    private Resource<GifData> wrapped;
    private GifDrawableResource resource;
    private GifData gifData;

    @Before
    public void setUp() {
        gifData = mock(GifData.class);
        wrapped = mock(Resource.class);
        when(wrapped.get()).thenReturn(gifData);
        resource = new GifDrawableResource(wrapped);
    }

    @Test
    public void testReturnsDrawableFromWrappedResource() {
        GifDrawable expected = mock(GifDrawable.class);
        when(gifData.getDrawable()).thenReturn(expected);

        assertEquals(expected, resource.get());
    }

    @Test
    public void testReturnsWrappedSize() {
        final int size = 2134;
        when(wrapped.getSize()).thenReturn(size);

        assertEquals(size, resource.getSize());
    }

    @Test
    public void testRecyclesWrappedWhenRecycled() {
        resource.recycle();

        verify(wrapped).recycle();
    }

}
