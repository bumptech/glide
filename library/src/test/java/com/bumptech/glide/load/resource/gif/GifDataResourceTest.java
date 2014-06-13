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
public class GifDataResourceTest {
    private GifDataResource resource;
    private GifData gifData;

    @Before
    public void setUp() {
        gifData = mock(GifData.class);
        resource = new GifDataResource(gifData);
    }

    @Test
    public void testSizeReturnsGifDecoderGifByteSize() {
        int size = 1234;
        when(gifData.getByteSize()).thenReturn(size);

        assertEquals(size, resource.getSize());
    }

    @Test
    public void testReturnsNonNullData() {
        assertNotNull(resource.get());
    }

    @Test
    public void testRecyclesGifDataOnRecycle() {
        resource.recycle();

        verify(gifData).recycle();
    }
}
