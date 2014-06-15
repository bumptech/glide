package com.bumptech.glide.load.resource.transcode;

import android.graphics.Bitmap;
import com.bumptech.glide.Resource;
import com.bumptech.glide.tests.Util;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class BitmapBytesTranscoderTest {
    private BitmapBytesTranscoderHarness harness;

    @Before()
    public void setUp() {
        harness = new BitmapBytesTranscoderHarness();
    }

    @Test
    public void testReturnsBytesOfGivenBitmap() {
        String transcodedDescription = harness.getTranscodedDescription();
        assertTrue(transcodedDescription, transcodedDescription.startsWith(harness.description));
    }

    @Test
    public void testUsesGivenQuality() {
        harness.quality = 66;
        String transcodedDescription = harness.getTranscodedDescription();
        assertTrue(transcodedDescription, transcodedDescription.contains(String.valueOf(harness.quality)));
    }

    @Test
    public void testUsesGivenFormat() {
        for (Bitmap.CompressFormat format : Bitmap.CompressFormat.values()) {
            harness.compressFormat = format;
            String transcodedDescription = harness.getTranscodedDescription();
            assertTrue(transcodedDescription, transcodedDescription.contains(format.name()));
        }
    }

    @Test
    public void testBitampResourceIsRecycled() {
        harness.getTranscodedDescription();

        verify(harness.bitmapResource).recycle();
    }

    @Test
    public void testHasValidId() {
        Util.assertClassHasValidId(BitmapBytesTranscoder.class,
                new BitmapBytesTranscoder(harness.compressFormat, harness.quality).getId());
    }

    @SuppressWarnings("unchecked")
    private static class BitmapBytesTranscoderHarness {
        Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
        int quality = 100;
        final String description = "TestDescription";
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ALPHA_8);
        Resource<Bitmap> bitmapResource = mock(Resource.class);

        public BitmapBytesTranscoderHarness() {
            when(bitmapResource.get()).thenReturn(bitmap);
            Robolectric.shadowOf(bitmap).setDescription(description);
        }

        public String getTranscodedDescription() {
            BitmapBytesTranscoder transcoder = new BitmapBytesTranscoder(compressFormat, quality);
            Resource<byte[]> bytesResource = transcoder.transcode(bitmapResource);

            return new String(bytesResource.get());
        }
    }
}
