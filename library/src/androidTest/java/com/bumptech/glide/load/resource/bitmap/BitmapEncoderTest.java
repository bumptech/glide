package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.engine.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowBitmap;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18, shadows = { BitmapEncoderTest.AlphaShadowBitmap.class })
public class BitmapEncoderTest {
    private EncoderHarness harness;

    @Before
    public void setUp() {
        harness = new EncoderHarness();
    }

    @Test
    public void testBitmapIsEncoded() {
        String fakeBytes = harness.encode();

        assertContains(fakeBytes, Shadows.shadowOf(harness.bitmap)
                .getDescription());
    }

    @Test
    public void testBitmapIsEncodedWithGivenQuality() {
        int quality = 7;
        harness.setQuality(quality);

        String fakeBytes = harness.encode();

        assertContains(fakeBytes, String.valueOf(quality));
    }

    @Test
    public void testEncoderObeysNonNullCompressFormat() {
        Bitmap.CompressFormat format = Bitmap.CompressFormat.WEBP;
        harness.setFormat(format);

        String fakeBytes = harness.encode();

        assertContains(fakeBytes, format.toString());
    }

    @Test
    public void testEncoderEncodesJpegWithNullFormatAndBitmapWithoutAlpha() {
        harness.setFormat(null);
        harness.bitmap.setHasAlpha(false);

        String fakeBytes = harness.encode();

        assertContains(fakeBytes, Bitmap.CompressFormat.JPEG.toString());
    }

    @Test
    public void testEncoderEncodesPngWithNullFormatAndBitmapWithAlpha() {
        harness.setFormat(null);
        harness.bitmap.setHasAlpha(true);

        String fakeBytes = harness.encode();

        assertContains(fakeBytes, Bitmap.CompressFormat.PNG.toString());
    }

    @Test
    public void testReturnsTrueFromWrite() {
        BitmapEncoder encoder = new BitmapEncoder();
        assertTrue(encoder.encode(harness.resource, harness.os, harness.options));
    }

    @Test
    public void testEncodeStrategy_alwaysReturnsTransformed() {
        BitmapEncoder encoder = new BitmapEncoder();
        assertEquals(EncodeStrategy.TRANSFORMED, encoder.getEncodeStrategy(harness.options));
    }

    private static void assertContains(String string, String expected) {
        assertThat(string).contains(expected);
    }

    @SuppressWarnings("unchecked")
    private static class EncoderHarness {
        Resource<Bitmap> resource = mock(Resource.class);
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Map<String, Object> options = new HashMap<>();

        public EncoderHarness() {
            when(resource.get()).thenReturn(bitmap);
        }

        public void setQuality(int quality) {
            options.put(BitmapEncoder.KEY_COMPRESSION_QUALITY, quality);
        }

        public void setFormat(Bitmap.CompressFormat format) {
            options.put(BitmapEncoder.KEY_COMPRESSION_FORMAT, format);
        }

        public String encode() {
            BitmapEncoder encoder = new BitmapEncoder();
            encoder.encode(resource, os, options);
            return new String(os.toByteArray());
        }
    }

    @Implements(Bitmap.class)
    public static class AlphaShadowBitmap extends ShadowBitmap {
        private boolean hasAlpha;

        @SuppressWarnings("unused")
        @Implementation
        public void setHasAlpha(boolean hasAlpha) {
            this.hasAlpha = hasAlpha;
        }

        @SuppressWarnings("unused")
        @Implementation
        public boolean hasAlpha() {
            return hasAlpha;
        }
    }
}
