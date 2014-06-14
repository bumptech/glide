package com.bumptech.glide.load.resource.transcode;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.resource.gif.GifData;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GifBitmapWrapperDrawableTranscoderTest {
    private GifBitmapWrapperDrawableTranscoder transcoder;
    private ResourceTranscoder<Bitmap, Drawable> bitmapTranscoder;
    private ResourceTranscoder<GifData, Drawable> gifDataTranscoder;

    @Before
    public void setUp() {
        bitmapTranscoder = mock(ResourceTranscoder.class);
        gifDataTranscoder = mock(ResourceTranscoder.class);
        transcoder = new GifBitmapWrapperDrawableTranscoder(bitmapTranscoder, gifDataTranscoder);
    }

    @Test
    public void testReturnsDrawableFromBitmapTranscoderIfGifBitmapHasBitmap() {
        GifBitmapWithBitmapHarness harness = new GifBitmapWithBitmapHarness();
        when(bitmapTranscoder.transcode(eq(harness.bitmapResource))).thenReturn(harness.expected);

        assertEquals(harness.expected, transcoder.transcode(harness.gifBitmapResource));
    }

    @Test
    public void testReturnsDrawableFromGifTranscoderIfGifBitmapHasGif() {
        GifBitmapWithGifHarness harness = new GifBitmapWithGifHarness();
        when(gifDataTranscoder.transcode(eq(harness.gifResource))).thenReturn(harness.expected);

        assertEquals(harness.expected, transcoder.transcode(harness.gifBitmapResource));
    }

    @Test
    public void testHasNonNullId() {
        assertNotNull(transcoder.getId());
    }

    private static class TranscoderHarness {
        Resource<GifBitmapWrapper> gifBitmapResource = mock(Resource.class);
        GifBitmapWrapper gifBitmap = mock(GifBitmapWrapper.class);
        Resource<Drawable> expected = mock(Resource.class);

        public TranscoderHarness() {
            when(gifBitmapResource.get()).thenReturn(gifBitmap);
        }
    }

    private static class GifBitmapWithBitmapHarness extends TranscoderHarness {
        Resource<Bitmap> bitmapResource = mock(Resource.class);

        public GifBitmapWithBitmapHarness() {
            super();
            when(gifBitmap.getBitmapResource()).thenReturn(bitmapResource);
        }
    }

    private static class GifBitmapWithGifHarness extends TranscoderHarness {
        GifData gifData = mock(GifData.class);
        Resource<GifData> gifResource = mock(Resource.class);

        public GifBitmapWithGifHarness() {
            super();
            when(gifResource.get()).thenReturn(gifData);
            when(gifBitmap.getGifResource()).thenReturn(gifResource);
        }
    }
}
