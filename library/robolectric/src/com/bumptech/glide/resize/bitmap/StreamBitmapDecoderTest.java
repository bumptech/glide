package com.bumptech.glide.resize.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.resize.Resource;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.resize.load.DecodeFormat;
import com.bumptech.glide.resize.load.Downsampler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class StreamBitmapDecoderTest {
    private DecoderHarness harness;

    @Before
    public void setUp() {
        harness = new DecoderHarness();
    }

    @Test
    public void testNonNullResourceIsReturned() {
        when(harness.downsampler.decode(eq(harness.source), eq(harness.bitmapPool), eq(harness.width),
                eq(harness.height), eq(harness.decodeFormat))).thenReturn(harness.result);
        assertNotNull(harness.decode());
    }

    @Test
    public void testNullResourceIsReturnedForNullBitmap() {
        when(harness.downsampler.decode(eq(harness.source), eq(harness.bitmapPool), eq(harness.width),
                eq(harness.height), eq(harness.decodeFormat))).thenReturn(null);
        assertNull(harness.decode());
    }


    private static class DecoderHarness {
        Downsampler downsampler = mock(Downsampler.class);
        BitmapPool bitmapPool = mock(BitmapPool.class);
        DecodeFormat decodeFormat = DecodeFormat.ALWAYS_ARGB_8888;
        InputStream source = new ByteArrayInputStream(new byte[0]);
        int width = 100;
        int height = 100;
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        StreamBitmapDecoder decoder = new StreamBitmapDecoder(downsampler, bitmapPool, decodeFormat);

        public DecoderHarness() {
        }

        public Resource decode() {
            return decoder.decode(source, width, height);
        }

    }
}
