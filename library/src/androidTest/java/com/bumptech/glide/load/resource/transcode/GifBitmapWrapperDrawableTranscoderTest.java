package com.bumptech.glide.load.resource.transcode;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapper;
import com.bumptech.glide.tests.Util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class GifBitmapWrapperDrawableTranscoderTest {
    private GifBitmapWrapperDrawableTranscoder transcoder;
    private ResourceTranscoder<Bitmap, GlideBitmapDrawable> bitmapTranscoder;

    @Before
    public void setUp() {
        bitmapTranscoder = mock(ResourceTranscoder.class);
        transcoder = new GifBitmapWrapperDrawableTranscoder(bitmapTranscoder);
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

        assertEquals(harness.gifResource, transcoder.transcode(harness.gifBitmapResource));
    }

    @Test
    public void testHasValid() {
        Util.assertClassHasValidId(GifBitmapWrapperDrawableTranscoder.class, transcoder.getId());
    }

    private static class TranscoderHarness {
        Resource<GifBitmapWrapper> gifBitmapResource = mock(Resource.class);
        GifBitmapWrapper gifBitmap = mock(GifBitmapWrapper.class);
        Resource<GlideBitmapDrawable> expected = mock(Resource.class);

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
        GifDrawable gifDrawable = mock(GifDrawable.class);
        Resource<GifDrawable> gifResource = mock(Resource.class);

        public GifBitmapWithGifHarness() {
            super();
            when(gifResource.get()).thenReturn(gifDrawable);
            when(gifBitmap.getGifResource()).thenReturn(gifResource);
        }
    }
}
