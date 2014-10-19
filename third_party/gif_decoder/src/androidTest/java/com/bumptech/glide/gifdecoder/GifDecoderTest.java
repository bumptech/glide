package com.bumptech.glide.gifdecoder;

import android.graphics.Bitmap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link com.bumptech.glide.gifdecoder.GifDecoder}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class GifDecoderTest {

    private MockProvider provider;

    @Before
    public void setUp() {
        provider = new MockProvider();
    }

    @Test
    public void testCanDecodeFramesFromTestGif() {
        byte[] data = TestUtil.readResourceData("partial_gif_decode.gif");
        GifHeaderParser headerParser = new GifHeaderParser();
        headerParser.setData(data);
        GifHeader header = headerParser.parseHeader();
        GifDecoder decoder = new GifDecoder(provider);
        decoder.setData(header, data);
        decoder.advance();
        Bitmap bitmap = decoder.getNextFrame();
        assertNotNull(bitmap);
        assertEquals(GifDecoder.STATUS_OK, decoder.getStatus());
    }

    private static class MockProvider implements GifDecoder.BitmapProvider {

        @Override
        public Bitmap obtain(int width, int height, Bitmap.Config config) {
            Bitmap result = Bitmap.createBitmap(width, height, config);
            Robolectric.shadowOf(result).setMutable(true);
            return result;
        }
    }
}
