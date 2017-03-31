package com.bumptech.glide.gifdecoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Robolectric.shadowOf;

import android.graphics.Bitmap;

import com.bumptech.glide.testutil.TestUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowBitmap;

import java.io.IOException;
import java.util.Arrays;

/**
 * Tests for {@link com.bumptech.glide.gifdecoder.GifDecoder}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class GifDecoderTest {

    private MockProvider provider;

    @Before
    public void setUp() {
        provider = new MockProvider();
    }

    @Test
    public void testCanDecodeFramesFromTestGif() throws IOException {
        byte[] data = TestUtil.resourceToBytes(getClass(), "partial_gif_decode.gif");
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

    @Test
    public void testFrameIndexStartsAtNegativeOne() {
        GifHeader gifheader = new GifHeader();
        gifheader.frameCount = 4;
        byte[] data = new byte[0];
        GifDecoder decoder = new GifDecoder(provider);
        decoder.setData(gifheader, data);
        assertEquals(-1, decoder.getCurrentFrameIndex());
    }

    @Test
    public void testTotalIterationCountIsOneIfNetscapeLoopCountDoesntExist() {
        GifHeader gifheader = new GifHeader();
        gifheader.loopCount = GifHeader.NETSCAPE_LOOP_COUNT_DOES_NOT_EXIST;
        byte[] data = new byte[0];
        GifDecoder decoder = new GifDecoder(provider);
        decoder.setData(gifheader, data);
        assertEquals(1, decoder.getTotalIterationCount());
    }

    @Test
    public void testTotalIterationCountIsForeverIfNetscapeLoopCountIsForever() {
        GifHeader gifheader = new GifHeader();
        gifheader.loopCount = GifHeader.NETSCAPE_LOOP_COUNT_FOREVER;
        byte[] data = new byte[0];
        GifDecoder decoder = new GifDecoder(provider);
        decoder.setData(gifheader, data);
        assertEquals(GifDecoder.TOTAL_ITERATION_COUNT_FOREVER, decoder.getTotalIterationCount());
    }

    @Test
    public void testTotalIterationCountIsTwoIfNetscapeLoopCountIsOne() {
        GifHeader gifheader = new GifHeader();
        gifheader.loopCount = 1;
        byte[] data = new byte[0];
        GifDecoder decoder = new GifDecoder(provider);
        decoder.setData(gifheader, data);
        assertEquals(2, decoder.getTotalIterationCount());
    }

    @Test
    public void testAdvanceIncrementsFrameIndex() {
        GifHeader gifheader = new GifHeader();
        gifheader.frameCount = 4;
        byte[] data = new byte[0];
        GifDecoder decoder = new GifDecoder(provider);
        decoder.setData(gifheader, data);
        decoder.advance();
        assertEquals(0, decoder.getCurrentFrameIndex());
    }

    @Test
    public void testAdvanceWrapsIndexBackToZero() {
        GifHeader gifheader = new GifHeader();
        gifheader.frameCount = 2;
        byte[] data = new byte[0];
        GifDecoder decoder = new GifDecoder(provider);
        decoder.setData(gifheader, data);
        decoder.advance();
        decoder.advance();
        decoder.advance();
        assertEquals(0, decoder.getCurrentFrameIndex());
    }

    @Test
    public void testSettingDataResetsFramePointer() {
        GifHeader gifheader = new GifHeader();
        gifheader.frameCount = 4;
        byte[] data = new byte[0];
        GifDecoder decoder = new GifDecoder(provider);
        decoder.setData(gifheader, data);
        decoder.advance();
        decoder.advance();
        assertEquals(1, decoder.getCurrentFrameIndex());

        decoder.setData(gifheader, data);
        assertEquals(-1, decoder.getCurrentFrameIndex());
    }

    @Test
    @Config(shadows = { CustomShadowBitmap.class })
    public void testFirstFrameMustClearBeforeDrawingWhenLastFrameIsDisposalBackground()
            throws IOException {
        byte[] data = TestUtil.resourceToBytes(getClass(), "transparent_disposal_background.gif");
        GifHeaderParser headerParser = new GifHeaderParser();
        headerParser.setData(data);
        GifHeader header = headerParser.parseHeader();
        GifDecoder decoder = new GifDecoder(provider);
        decoder.setData(header, data);
        decoder.advance();
        Bitmap firstFrame = decoder.getNextFrame();
        decoder.advance();
        decoder.getNextFrame();
        decoder.advance();
        Bitmap firstFrameTwice = decoder.getNextFrame();
        assertTrue(Arrays.equals((((CustomShadowBitmap) shadowOf(firstFrame))).getPixels(),
                (((CustomShadowBitmap) shadowOf(firstFrameTwice))).getPixels()));
    }

    @Test
    @Config(shadows = { CustomShadowBitmap.class })
    public void testFirstFrameMustClearBeforeDrawingWhenLastFrameIsDisposalNone() throws IOException {
        byte[] data = TestUtil.resourceToBytes(getClass(), "transparent_disposal_none.gif");
        GifHeaderParser headerParser = new GifHeaderParser();
        headerParser.setData(data);
        GifHeader header = headerParser.parseHeader();
        GifDecoder decoder = new GifDecoder(provider);
        decoder.setData(header, data);
        decoder.advance();
        Bitmap firstFrame = decoder.getNextFrame();
        decoder.advance();
        decoder.getNextFrame();
        decoder.advance();
        Bitmap firstFrameTwice = decoder.getNextFrame();
        assertTrue(Arrays.equals((((CustomShadowBitmap) shadowOf(firstFrame))).getPixels(),
                (((CustomShadowBitmap) shadowOf(firstFrameTwice))).getPixels()));
    }

    /**
     * Preserve generated bitmap data for checking.
     */
    @Implements(Bitmap.class)
    public static class CustomShadowBitmap extends ShadowBitmap {

        private int[] pixels;

        @Implementation
        public void setPixels(int[] pixels, int offset, int stride,
                int x, int y, int width, int height) {
            this.pixels = new int[pixels.length];
            System.arraycopy(pixels, 0, this.pixels, 0, this.pixels.length);
        }

        public int[] getPixels() {
            return pixels;
        }
    }

    private static class MockProvider implements GifDecoder.BitmapProvider {

        @Override
        public Bitmap obtain(int width, int height, Bitmap.Config config) {
            Bitmap result = Bitmap.createBitmap(width, height, config);
            Robolectric.shadowOf(result).setMutable(true);
            return result;
        }

        @Override
        public void release(Bitmap bitmap) {
            // Do nothing.
        }
    }
}
