package com.bumptech.glide.request.target;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class ImageViewTargetFactoryTest {
    private ImageViewTargetFactory factory;
    private ImageView view;

    @Before
    public void setUp() {
        factory = new ImageViewTargetFactory();
        view = new ImageView(Robolectric.application);
    }

    @Test
    public void testReturnsTargetForBitmaps() {
        Bitmap bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        Target<Bitmap> target = factory.buildTarget(view, Bitmap.class);
        target.onResourceReady(bitmap, null);
        assertTrue(target instanceof BitmapImageViewTarget);
    }

    @Test
    public void testReturnsTargetForGlideDrawables() {
        GlideDrawable glideDrawable = mock(GlideDrawable.class);
        Target<GlideDrawable> target = factory.buildTarget(view, GlideDrawable.class);
        target.onResourceReady(glideDrawable, null);
        assertTrue(target instanceof GlideDrawableImageViewTarget);
    }

    @Test
    public void testReturnsTargetForGifDrawables() {
        GifDrawable gifDrawable = mock(GifDrawable.class);
        Target target = factory.buildTarget(view, GifDrawable.class);
        target.onResourceReady(gifDrawable, null);
        assertTrue(target instanceof GlideDrawableImageViewTarget);
    }

    @Test
    public void testReturnsTargetForGlideBitmapDrawables() {
        GlideBitmapDrawable drawable = mock(GlideBitmapDrawable.class);
        Target target = factory.buildTarget(view, GlideBitmapDrawable.class);
        target.onResourceReady(drawable, null);
        assertTrue(target instanceof GlideDrawableImageViewTarget);
    }

    @Test
    public void testReturnsTargetForBitmapDrawables() {
        BitmapDrawable drawable = new BitmapDrawable(Robolectric.application.getResources(),
                Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_4444));

        Target target = factory.buildTarget(view, BitmapDrawable.class);
        target.onResourceReady(drawable, null);
        assertTrue(target instanceof DrawableImageViewTarget);
    }

    @Test
    public void testReturnsTargetForDrawables() {
        Target<Drawable> target = factory.buildTarget(view, Drawable.class);
        target.onResourceReady(new ColorDrawable(Color.RED), null);
        assertTrue(target instanceof DrawableImageViewTarget);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsForUnknownType() {
        factory.buildTarget(view, Object.class);
    }
}
