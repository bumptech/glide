package com.bumptech.glide.load.resource.gif;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.UnitTransformation;

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

    @Test
    public void testSetsTransformationAsFrameTransformation() {
        Resource<GifDrawable> resource = mock(Resource.class);
        GifDrawable gifDrawable = mock(GifDrawable.class);
        Transformation<Bitmap> unitTransformation = UnitTransformation.get();
        when(gifDrawable.getFrameTransformation()).thenReturn(unitTransformation);
        when(gifDrawable.getIntrinsicWidth()).thenReturn(500);
        when(gifDrawable.getIntrinsicHeight()).thenReturn(500);
        when(resource.get()).thenReturn(gifDrawable);

        Bitmap firstFrame = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        when(gifDrawable.getFirstFrame()).thenReturn(firstFrame);

        final int width = 123;
        final int height = 456;
        Bitmap expectedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Resource<Bitmap> expectedResource = mock(Resource.class);
        when(expectedResource.get()).thenReturn(expectedBitmap);
        when(wrapped.transform(any(Resource.class), anyInt(), anyInt())).thenReturn(expectedResource);

        transformation.transform(resource, width, height);

        verify(gifDrawable).setFrameTransformation(any(Transformation.class), eq(expectedBitmap));
    }
}
