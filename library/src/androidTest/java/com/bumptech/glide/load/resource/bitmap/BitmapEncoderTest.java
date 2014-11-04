package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;
import static com.bumptech.glide.tests.Util.assertClassHasValidId;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.load.engine.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowBitmap;

import java.io.ByteArrayOutputStream;

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

        assertContains(fakeBytes, Robolectric.shadowOf(harness.bitmap)
                .getDescription());
    }

    @Test
    public void testBitmapIsEncodedWithGivenQuality() {
        harness.quality = 7;

        String fakeBytes = harness.encode();

        assertContains(fakeBytes, String.valueOf(harness.quality));
    }

    @Test
    public void testEncoderObeysNonNullCompressFormat() {
        harness.compressFormat = Bitmap.CompressFormat.WEBP;

        String fakeBytes = harness.encode();

        assertContains(fakeBytes, harness.compressFormat.toString());
    }

    @Test
    public void testEncoderEncodesJpegWithNullFormatAndBitmapWithoutAlpha() {
        harness.compressFormat = null;
        harness.bitmap.setHasAlpha(false);

        String fakeBytes = harness.encode();

        assertContains(fakeBytes, Bitmap.CompressFormat.JPEG.toString());
    }

    @Test
    public void testEncoderEncodesPngWithNullFormatAndBitmapWithAlpha() {
        harness.compressFormat = null;
        harness.bitmap.setHasAlpha(true);

        String fakeBytes = harness.encode();

        assertContains(fakeBytes, Bitmap.CompressFormat.PNG.toString());
    }

    @Test
    public void testReturnsTrueFromWrite() {
        BitmapEncoder encoder = new BitmapEncoder(harness.compressFormat, harness.quality);
        assertTrue(encoder.encode(harness.resource, harness.os));
    }

    @Test
    public void testReturnsValidId() {
        assertClassHasValidId(BitmapEncoder.class,
                new BitmapEncoder(harness.compressFormat, harness.quality).getId());
    }

    private static void assertContains(String string, String expected) {
        assertThat(string).contains(expected);
    }

    @SuppressWarnings("unchecked")
    private static class EncoderHarness {
        Bitmap.CompressFormat compressFormat = null;
        Resource<Bitmap> resource = mock(Resource.class);
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int quality = 100;

        public EncoderHarness() {
            when(resource.get()).thenReturn(bitmap);
        }

        public String encode() {
            BitmapEncoder encoder = new BitmapEncoder(compressFormat, quality);
            encoder.encode(resource, os);
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
