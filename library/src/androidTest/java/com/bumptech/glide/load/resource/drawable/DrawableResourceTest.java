package com.bumptech.glide.load.resource.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.load.resource.gif.GifDrawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class DrawableResourceTest {
    private TestDrawable drawable;
    private DrawableResource<TestDrawable> resource;

    @Before
    public void setUp() {
        drawable = mock(TestDrawable.class);
        resource = new DrawableResource<TestDrawable>(drawable) {
            @Override
            public int getSize() {
                return 0;
            }

            @Override
            protected void recycleInternal() { }
        };
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

        assertEquals(drawable, resource.get());
        assertEquals(expected, resource.get());

        verify(drawable).getConstantState();
        verify(constantState).newDrawable();
    }

    /** Just to have a type to test with which is not directly Drawable */
    private static class TestDrawable extends Drawable {
        @Override
        public void draw(Canvas canvas) {

        }

        @Override
        public void setAlpha(int alpha) {

        }

        @Override
        public void setColorFilter(ColorFilter cf) {

        }

        @Override
        public int getOpacity() {
            return 0;
        }
    }
}