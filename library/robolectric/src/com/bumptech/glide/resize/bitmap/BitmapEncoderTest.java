package com.bumptech.glide.resize.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.resize.Resource;
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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { BitmapEncoderTest.AlphaShadowBitmap.class })
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

    private static void assertContains(String string, String expected) {
        assertTrue("Expected '" + string + "' to contain '" + expected + "'",
                string.contains(expected));
    }

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

        @Implementation
        public void setHasAlpha(boolean hasAlpha) {
            this.hasAlpha = hasAlpha;
        }

        @Implementation
        public boolean hasAlpha() {
            return hasAlpha;
        }
    }
}
