package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.tests.GlideShadowLooper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = GlideShadowLooper.class)
public class GifResourceDecoderTest {
    private GifResourceDecoder decoder;
    private GifHeaderParser parser;
    private GifResourceDecoder.GifHeaderParserPool parserPool;

    @Before
    public void setUp() {
        BitmapPool bitmapPool = mock(BitmapPool.class);
        parser = mock(GifHeaderParser.class);
        parserPool = mock(GifResourceDecoder.GifHeaderParserPool.class);
        when(parserPool.obtain(any(byte[].class))).thenReturn(parser);
        decoder = new GifResourceDecoder(Robolectric.application, bitmapPool, parserPool);
    }

    @Test
    public void testReturnsNullIfParsedHeaderHasZeroFrames() throws IOException {
        GifHeader header = mock(GifHeader.class);
        when(parser.parseHeader()).thenReturn(header);
        when(header.getNumFrames()).thenReturn(0);

        assertNull(decoder.decode(new ByteArrayInputStream(new byte[0]), 100, 100));
    }

    @Test
    public void testReturnsParserToPool() throws IOException {
        when(parserPool.obtain(any(byte[].class))).thenReturn(parser);
        when(parser.parseHeader()).thenReturn(mock(GifHeader.class));

        decoder.decode(new ByteArrayInputStream(new byte[0]), 100, 100);
        verify(parserPool).release(eq(parser));
    }

    @Test
    public void testReturnsParserToPoolWhenParserThrows() {
        when(parser.parseHeader()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw new IOException("Test");
            }
        });
        try {
            decoder.decode(new ByteArrayInputStream(new byte[0]), 100, 100);
        } catch (IOException e) {
            // Expected.
        }

        verify(parserPool).release(eq(parser));
    }

    @Test
    public void testHasValidId() {
        assertEquals("", decoder.getId());
    }
}
