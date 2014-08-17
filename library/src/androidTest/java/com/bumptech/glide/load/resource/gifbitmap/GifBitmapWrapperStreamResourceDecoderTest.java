package com.bumptech.glide.load.resource.gifbitmap;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GifBitmapWrapperStreamResourceDecoderTest {
    ResourceDecoder<ImageVideoWrapper, GifBitmapWrapper> gifBitmapDecoder;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        gifBitmapDecoder = mock(ResourceDecoder.class);
    }

    @Test
    public void testHasValidId() {
        String id = "asdf225";
        when(gifBitmapDecoder.getId()).thenReturn(id);
        assertEquals(id, gifBitmapDecoder.getId());
    }

}
