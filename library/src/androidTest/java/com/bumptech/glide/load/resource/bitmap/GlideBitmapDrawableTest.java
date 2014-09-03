package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class GlideBitmapDrawableTest {

    private Bitmap bitmap;
    private GlideBitmapDrawable drawable;

    @Before
    public void setUp() {
        bitmap = Bitmap.createBitmap(123, 456, Bitmap.Config.ARGB_8888);
        drawable = new GlideBitmapDrawable(Robolectric.application.getResources(), bitmap);
    }

    @Test
    public void testReturnsWidthOfGivenBitmap() {
        assertEquals(bitmap.getWidth(), drawable.getIntrinsicWidth());
    }

    @Test
    public void testReturnsHeightOfGivenBitmap() {
        assertEquals(bitmap.getHeight(), drawable.getIntrinsicHeight());
    }

    @Test
    public void testReturnsNotAnimated() {
        assertFalse(drawable.isAnimated());
    }

    @Test
    public void testReturnsNotRunning() {
        assertFalse(drawable.isRunning());
    }

    @Test
    public void testReturnsOpacityOpaqueIfBitmapDoesNotHaveAlpha() {
        bitmap.setHasAlpha(false);
        drawable.setAlpha(255);
        assertEquals(PixelFormat.OPAQUE, drawable.getOpacity());
    }

    @Test
    public void testReturnsOpacityTranslucentIfAlphaIsSet() {
        bitmap.setHasAlpha(false);
        drawable.setAlpha(100);
        assertEquals(PixelFormat.TRANSLUCENT, drawable.getOpacity());
    }

    @Test
    public void testReturnsNonNullConstantState() {
        assertNotNull(drawable.getConstantState());
    }

    @Test
    public void testConstantStateReturnsNewGlideBitmapDrawable() {
        Drawable newDrawable = drawable.getConstantState().newDrawable();
        assertThat(newDrawable, instanceOf(GlideBitmapDrawable.class));
    }

    @Test
    public void testDrawableReturnedByConstantStateWrapsSameBitmap() {
        GlideBitmapDrawable newDrawable = (GlideBitmapDrawable) drawable.getConstantState().newDrawable();
        assertEquals(drawable.getBitmap(), newDrawable.getBitmap());
    }

    @Test
    public void testMutateReturnsNewDrawable() {
        assertNotSame(drawable, drawable.mutate());
    }

    @Test
    public void testMutatedDrawableIsGlideBitmapDrawable() {
        Drawable newDrawable = drawable.mutate();
        assertThat(newDrawable, instanceOf(GlideBitmapDrawable.class));
    }

    @Test
    public void testMutatedDrawableWrapsSameBitmap() {
        GlideBitmapDrawable mutated = (GlideBitmapDrawable) drawable.mutate();
        assertEquals(drawable.getBitmap(), mutated.getBitmap());
    }
}