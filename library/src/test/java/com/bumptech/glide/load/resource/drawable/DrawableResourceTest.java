package com.bumptech.glide.load.resource.drawable;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import com.bumptech.glide.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class DrawableResourceTest {
    private ColorDrawable drawable;
    private DrawableResource resource;
    private Resource wrapped;

    @Before
    public void setUp() {
        drawable = new ColorDrawable(Color.RED);
        wrapped = mock(Resource.class);
        resource = new DrawableResource(drawable, wrapped);
    }

    @Test
    public void testReturnsGivenSize() {
        final int size = 100;
        when(wrapped.getSize()).thenReturn(size);
        assertEquals(size, resource.getSize());
    }

    @Test
    public void testGetReturnsGivenDrawable() {
        assertEquals(drawable, resource.get());
    }

    @Test
    public void testRecyclesWrappedWhenRecycled() {
        resource.recycleInternal();

        verify(wrapped).recycle();
    }
}
