package com.bumptech.glide.load.resource.transcode;

import android.graphics.Bitmap;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.tests.Util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GlideBitmapDrawableTranscoderTest {
    private GlideBitmapDrawableTranscoder transcoder;

    @Before
    public void setUp() {
        transcoder = new GlideBitmapDrawableTranscoder(Robolectric.application.getResources(), mock(BitmapPool.class));
    }

    @Test
    public void testHasValidId() {
        Util.assertClassHasValidId(GlideBitmapDrawableTranscoder.class, transcoder.getId());
    }

    @Test
    public void testReturnsBitmapDrawableResourceContainingGivenBitmap() {
        Bitmap expected = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Resource<Bitmap> resource = mock(Resource.class);
        when(resource.get()).thenReturn(expected);

        Resource<GlideBitmapDrawable> transcoded = transcoder.transcode(resource);

        assertEquals(expected, transcoded.get().getBitmap());
    }
}
