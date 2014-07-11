package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.tests.GlideShadowLooper;
import com.bumptech.glide.tests.Util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = GlideShadowLooper.class)
public class GifResourceDecoderTest {
    private GifResourceDecoder decoder;
    private GifHeaderParser parser;

    @Before
    public void setUp() {
        BitmapPool bitmapPool = mock(BitmapPool.class);
        parser = mock(GifHeaderParser.class);
        decoder = new GifResourceDecoder(Robolectric.application, bitmapPool, parser);
    }

    @Test
    public void testReturnsNullIfParsedHeaderHasZeroFrames() throws IOException {
        GifHeader header = mock(GifHeader.class);
        when(parser.setData(any(byte[].class))).thenReturn(parser);
        when(parser.parseHeader()).thenReturn(header);
        when(header.getNumFrames()).thenReturn(0);

        assertNull(decoder.decode(new ByteArrayInputStream(new byte[0]), 100, 100));
    }

    @Test
    public void testHasValidId() {
        Util.assertClassHasValidId(GifResourceDecoder.class, decoder.getId());
    }
}
