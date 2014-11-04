package com.bumptech.glide.load.resource.gif;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.tests.GlideShadowLooper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18, shadows = GlideShadowLooper.class)
public class GifResourceDecoderTest {
    private GifResourceDecoder decoder;
    private GifHeaderParser parser;
    private GifResourceDecoder.GifHeaderParserPool parserPool;
    private GifResourceDecoder.GifDecoderPool decoderPool;
    private GifDecoder gifDecoder;
    private GifHeader gifHeader;

    @Before
    public void setUp() {
        BitmapPool bitmapPool = mock(BitmapPool.class);

        gifHeader = Mockito.spy(new GifHeader());
        parser = mock(GifHeaderParser.class);
        when(parser.parseHeader()).thenReturn(gifHeader);
        parserPool = mock(GifResourceDecoder.GifHeaderParserPool.class);
        when(parserPool.obtain(any(byte[].class))).thenReturn(parser);

        gifDecoder = mock(GifDecoder.class);
        decoderPool = mock(GifResourceDecoder.GifDecoderPool.class);
        when(decoderPool.obtain(any(GifDecoder.BitmapProvider.class))).thenReturn(gifDecoder);

        decoder = new GifResourceDecoder(Robolectric.application, bitmapPool, parserPool, decoderPool);
    }

    @Test
    public void testReturnsNullIfParsedHeaderHasZeroFrames() throws IOException {
        when(gifHeader.getNumFrames()).thenReturn(0);

        assertNull(decoder.decode(new ByteArrayInputStream(new byte[0]), 100, 100));
    }

    @Test
    public void testReturnsNullIfParsedHeaderHasFormatError() {
        when(gifHeader.getStatus()).thenReturn(GifDecoder.STATUS_FORMAT_ERROR);

        assertNull(decoder.decode(new ByteArrayInputStream(new byte[0]), 100, 100));
    }

    @Test
    public void testReturnsNullIfParsedHeaderHasOpenError() {
        when(gifHeader.getStatus()).thenReturn(GifDecoder.STATUS_OPEN_ERROR);

        assertNull(decoder.decode(new ByteArrayInputStream(new byte[0]), 100, 100));
    }

    @Test
    public void testReturnsParserToPool() throws IOException {
        decoder.decode(new ByteArrayInputStream(new byte[0]), 100, 100);
        verify(parserPool).release(eq(parser));
    }

    @Test
    public void testReturnsParserToPoolWhenParserThrows() {
        when(parser.parseHeader()).thenThrow(new RuntimeException("Test"));
        try {
            decoder.decode(new ByteArrayInputStream(new byte[0]), 100, 100);
            fail("Failed to receive expected exception");
        } catch (RuntimeException e) {
            // Expected.
        }

        verify(parserPool).release(eq(parser));
    }

    @Test
    public void testDecodesFirstFrameAndReturnsGifDecoderToPool() {
        when(gifHeader.getNumFrames()).thenReturn(1);
        when(gifHeader.getStatus()).thenReturn(GifDecoder.STATUS_OK);
        when(gifDecoder.getNextFrame()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));

        byte[] data = new byte[100];
        decoder.decode(new ByteArrayInputStream(data), 100, 100);

        InOrder order = inOrder(decoderPool, gifDecoder);
        order.verify(decoderPool).obtain(any(GifDecoder.BitmapProvider.class));
        order.verify(gifDecoder).setData(eq(gifHeader), eq(data));
        order.verify(gifDecoder).advance();
        order.verify(gifDecoder).getNextFrame();
        order.verify(decoderPool).release(eq(gifDecoder));
    }

    @Test
    public void testReturnsGifDecoderToPoolWhenDecoderThrows() {
        when(gifHeader.getNumFrames()).thenReturn(1);
        when(gifHeader.getStatus()).thenReturn(GifDecoder.STATUS_OK);
        when(gifDecoder.getNextFrame()).thenThrow(new RuntimeException("test"));
        try {
            decoder.decode(new ByteArrayInputStream(new byte[0]), 100, 100);
            fail("Failed to receive expected exception");
        } catch (RuntimeException e) {
            // Expected.
        }

        verify(decoderPool).release(eq(gifDecoder));
    }

    @Test
    public void testReturnsNullIfGifDecoderFailsToDecodeFirstFrame() {
        when(gifHeader.getNumFrames()).thenReturn(1);
        when(gifHeader.getStatus()).thenReturn(GifDecoder.STATUS_OK);
        when(gifDecoder.getNextFrame()).thenReturn(null);

        assertNull(decoder.decode(new ByteArrayInputStream(new byte[0]), 100, 100));
    }

    @Test
    public void testReturnsGifDecoderToPoolWhenGifDecoderReturnsNullFirstFrame() {
        when(gifHeader.getNumFrames()).thenReturn(1);
        when(gifHeader.getStatus()).thenReturn(GifDecoder.STATUS_OK);
        when(gifDecoder.getNextFrame()).thenReturn(null);

        decoder.decode(new ByteArrayInputStream(new byte[0]), 100, 100);

        verify(decoderPool).release(eq(gifDecoder));
    }

    @Test
    public void testCanObtainNonNullDecoderFromPool() {
        GifDecoder.BitmapProvider provider = mock(GifDecoder.BitmapProvider.class);
        GifResourceDecoder.GifDecoderPool pool = new GifResourceDecoder.GifDecoderPool();
        assertNotNull(pool.obtain(provider));
    }

    @Test
    public void testCanPutAndObtainDecoderFromPool() {
        GifResourceDecoder.GifDecoderPool pool = new GifResourceDecoder.GifDecoderPool();
        pool.release(gifDecoder);
        GifDecoder fromPool = pool.obtain(mock(GifDecoder.BitmapProvider.class));
        assertEquals(gifDecoder, fromPool);
    }

    @Test
    public void testDecoderPoolClearsDecoders() {
        GifResourceDecoder.GifDecoderPool pool = new GifResourceDecoder.GifDecoderPool();
        pool.release(gifDecoder);
        verify(gifDecoder).clear();
    }

    @Test
    public void testHasValidId() {
        assertEquals("", decoder.getId());
    }
}
