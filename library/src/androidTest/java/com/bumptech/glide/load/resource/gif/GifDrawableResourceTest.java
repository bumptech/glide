package com.bumptech.glide.load.resource.gif;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.util.Util;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GifDrawableResourceTest {
    private GifDrawable drawable;
    private GifDrawableResource resource;

    @Before
    public void setUp() {
        drawable = mock(GifDrawable.class);
        resource = new GifDrawableResource(drawable);
    }

    @Test
    public void testReturnsDrawableOnFirstGet() {
        assertEquals(drawable, resource.get());
    }

    @Test
    public void testReturnsNewDrawableOnSecondGet() {
        GifDrawable expected = mock(GifDrawable.class);
        Drawable.ConstantState constantState = mock(Drawable.ConstantState.class);
        when(constantState.newDrawable()).thenReturn(expected);
        when(drawable.getConstantState()).thenReturn(constantState);

        resource.get();
        assertEquals(expected, resource.get());
    }

    @Test
    public void testReturnsDrawableSizePlusFirstFrameSize() {
        final int size = 2134;
        Bitmap firstFrame = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        when(drawable.getFirstFrame()).thenReturn(firstFrame);
        when(drawable.getData()).thenReturn(new byte[size]);

        assertEquals(size + Util.getBitmapByteSize(firstFrame), resource.getSize());
    }

    @Test
    public void testStopsAndThenRecyclesDrawableWhenRecycled() {
        resource.recycle();

        InOrder inOrder = inOrder(drawable);
        inOrder.verify(drawable).stop();
        inOrder.verify(drawable).recycle();
    }

}
