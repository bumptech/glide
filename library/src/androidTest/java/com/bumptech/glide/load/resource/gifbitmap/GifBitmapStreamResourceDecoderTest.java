package com.bumptech.glide.load.resource.gifbitmap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.model.ImageVideoWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@RunWith(JUnit4.class)
public class GifBitmapStreamResourceDecoderTest {
    ResourceDecoder<ImageVideoWrapper, GifBitmapWrapper> gifBitmapDecoder;
    private GifBitmapWrapperStreamResourceDecoder decoder;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        gifBitmapDecoder = mock(ResourceDecoder.class);
        decoder = new GifBitmapWrapperStreamResourceDecoder(gifBitmapDecoder);
    }

    @Test
    public void testReturnsWrappedDecoderResult() throws IOException {
        int width = 100;
        int height = 110;
        Resource<GifBitmapWrapper> expected = mock(Resource.class);
        when(gifBitmapDecoder.decode(any(ImageVideoWrapper.class), eq(width), eq(height))).thenReturn(expected);

        assertEquals(expected, decoder.decode(new ByteArrayInputStream(new byte[0]), width, height));
    }

    @Test
    public void testPassesGivenInputStreamWrappedAsImageVideoWrapper() throws IOException {
        final InputStream expected = new ByteArrayInputStream(new byte[0]);
        when(gifBitmapDecoder.decode(any(ImageVideoWrapper.class), anyInt(), anyInt()))
                .thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ImageVideoWrapper wrapper = (ImageVideoWrapper) invocation.getArguments()[0];
                assertEquals(expected, wrapper.getStream());
                return null;
            }
        });

        decoder.decode(expected, 1, 2);
    }

    @Test
    public void testReturnsWrappedId() {
        String id = "fakeTestId";
        when(gifBitmapDecoder.getId()).thenReturn(id);

        assertEquals(id, decoder.getId());
    }
}
