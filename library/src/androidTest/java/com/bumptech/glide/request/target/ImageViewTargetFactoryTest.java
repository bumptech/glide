package com.bumptech.glide.request.target;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

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
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
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
        assertThat(target).isInstanceOf(BitmapImageViewTarget.class);
    }

    @Test
    public void testReturnsTargetForGlideDrawables() {
        GlideDrawable glideDrawable = mock(GlideDrawable.class);
        Target<GlideDrawable> target = factory.buildTarget(view, GlideDrawable.class);
        target.onResourceReady(glideDrawable, null);
        assertThat(target).isInstanceOf(GlideDrawableImageViewTarget.class);
    }

    @Test
    public void testReturnsTargetForGifDrawables() {
        GifDrawable gifDrawable = mock(GifDrawable.class);
        Target target = factory.buildTarget(view, GifDrawable.class);
        target.onResourceReady(gifDrawable, null);
        assertThat(target).isInstanceOf(GlideDrawableImageViewTarget.class);
    }

    @Test
    public void testReturnsTargetForGlideBitmapDrawables() {
        GlideBitmapDrawable drawable = mock(GlideBitmapDrawable.class);
        Target target = factory.buildTarget(view, GlideBitmapDrawable.class);
        target.onResourceReady(drawable, null);
        assertThat(target).isInstanceOf(GlideDrawableImageViewTarget.class);
    }

    @Test
    public void testReturnsTargetForBitmapDrawables() {
        BitmapDrawable drawable = new BitmapDrawable(Robolectric.application.getResources(),
                Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_4444));

        Target target = factory.buildTarget(view, BitmapDrawable.class);
        target.onResourceReady(drawable, null);
        assertThat(target).isInstanceOf(DrawableImageViewTarget.class);
    }

    @Test
    public void testReturnsTargetForDrawables() {
        Target<Drawable> target = factory.buildTarget(view, Drawable.class);
        target.onResourceReady(new ColorDrawable(Color.RED), null);
        assertThat(target).isInstanceOf(DrawableImageViewTarget.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsForUnknownType() {
        factory.buildTarget(view, Object.class);
    }
}
