package com.bumptech.glide.load.resource.gif;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class GifDrawableTransformationTest {
    Transformation<Bitmap> wrapped;
    GifDrawableTransformation transformation;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        wrapped = mock(Transformation.class);
        BitmapPool bitmapPool = mock(BitmapPool.class);
        transformation = new GifDrawableTransformation(wrapped, bitmapPool);
    }

    @Test
    public void testReturnsWrappedTransformationId() {
        final String id = "testId";
        when(wrapped.getId()).thenReturn(id);

        assertEquals(id, transformation.getId());
    }
}
