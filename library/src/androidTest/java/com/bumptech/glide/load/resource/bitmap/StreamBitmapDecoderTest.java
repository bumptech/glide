package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.tests.Util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
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

    @Test
    public void testHasValidId() {
        String downsamplerId = "downsamplerId";
        when(harness.downsampler.getId()).thenReturn(downsamplerId);

        String actualId = harness.decoder.getId();
        assertThat(actualId).contains(downsamplerId);
        assertThat(actualId).contains(harness.decodeFormat.toString());
        assertThat(actualId).contains(Util.getExpectedClassId(StreamBitmapDecoder.class));
    }

    private static class DecoderHarness {
        Downsampler downsampler = mock(Downsampler.class);
        BitmapPool bitmapPool = mock(BitmapPool.class);
        DecodeFormat decodeFormat = DecodeFormat.PREFER_ARGB_8888;
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
