package com.bumptech.glide.load.resource.transcode;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.resource.gif.GifData;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GifBitmapDrawableTranscoderTest {
    private GifBitmapDrawableTranscoder transcoder;

    @Before
    public void setUp() {
        transcoder = new GifBitmapDrawableTranscoder(Robolectric.application);
    }

    @Test
    public void testReturnsBitmapDrawableIfGifBitmapHasBitmap() {
        GifBitmapWithBitmapHarness harness = new GifBitmapWithBitmapHarness();

        BitmapDrawable transcoded = (BitmapDrawable) transcoder.transcode(harness.gifBitmapResource).get();

        assertEquals(harness.expected, transcoded.getBitmap());
    }

    @Test
    public void testReturnedResourceHasBitmapSizeIfGifBitmapHasBitmap() {
        final int size = 100;
        GifBitmapWithBitmapHarness harness = new GifBitmapWithBitmapHarness();
        when(harness.bitmapResource.getSize()).thenReturn(size);

        Resource<Drawable> transcoded = transcoder.transcode(harness.gifBitmapResource);

        assertEquals(size, transcoded.getSize());
    }

    @Test
    public void testReturnsGifDrawableIfGifBitmapHasGif() {
        GifBitmapWithGifHarness harness = new GifBitmapWithGifHarness();

        Drawable transcoded = transcoder.transcode(harness.gifBitmapResource).get();

        assertEquals(harness.expected, transcoded);
    }

    @Test
    public void testReturnedResourceHasGifDrawableSizeIfGifBitmapHasGif() {
        final int size = 200;
        GifBitmapWithGifHarness harness = new GifBitmapWithGifHarness();
        when(harness.gifResource.getSize()).thenReturn(size);

        Resource<Drawable> transcoded = transcoder.transcode(harness.gifBitmapResource);

        assertEquals(size, transcoded.getSize());
    }

    @Test
    public void testHasNonNullId() {
        assertNotNull(transcoder.getId());
    }

    private static class TranscoderHarness {
        Resource<GifBitmap> gifBitmapResource = mock(Resource.class);
        GifBitmap gifBitmap = mock(GifBitmap.class);

        public TranscoderHarness() {
            when(gifBitmapResource.get()).thenReturn(gifBitmap);
        }
    }

    private static class GifBitmapWithBitmapHarness extends TranscoderHarness {
        Bitmap expected = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
        Resource<Bitmap> bitmapResource = mock(Resource.class);

        public GifBitmapWithBitmapHarness() {
            super();
            when(bitmapResource.get()).thenReturn(expected);
            when(gifBitmap.getBitmapResource()).thenReturn(bitmapResource);
        }
    }

    private static class GifBitmapWithGifHarness extends TranscoderHarness {
        GifDrawable expected = mock(GifDrawable.class);
        GifData gifData = mock(GifData.class);
        Resource<GifData> gifResource = mock(Resource.class);

        public GifBitmapWithGifHarness() {
            super();
            when(gifData.getDrawable()).thenReturn(expected);
            when(gifResource.get()).thenReturn(gifData);
            when(gifBitmap.getGifResource()).thenReturn(gifResource);
        }
    }
}
