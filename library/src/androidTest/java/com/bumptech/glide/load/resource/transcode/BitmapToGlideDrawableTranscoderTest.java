package com.bumptech.glide.load.resource.transcode;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class BitmapToGlideDrawableTranscoderTest {

    private GlideBitmapDrawableTranscoder wrapped;
    private BitmapToGlideDrawableTranscoder transcoder;

    @Before
    public void setUp() {
        wrapped = mock(GlideBitmapDrawableTranscoder.class);
        transcoder = new BitmapToGlideDrawableTranscoder(wrapped);
    }

    @Test
    public void testReturnsWrappedId() {
        final String expectedId = "fakeId";
        when(wrapped.getId()).thenReturn(expectedId);
        assertEquals(expectedId, transcoder.getId());
    }

    @Test
    public void testReturnsResourceFromWrapped() {
        Resource<Bitmap> toTranscode = mock(Resource.class);
        Resource<GlideBitmapDrawable> expected = mock(Resource.class);

        when(wrapped.transcode(eq(toTranscode))).thenReturn(expected);

        assertEquals(expected, transcoder.transcode(toTranscode));
    }
}