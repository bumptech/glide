package com.bumptech.glide.request.target;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class ImageViewTargetFactoryTest {
  private ImageViewTargetFactory factory;
  private ImageView view;

  @Before
  public void setUp() {
    factory = new ImageViewTargetFactory();
    view = new ImageView(ApplicationProvider.getApplicationContext());
  }

  @Test
  public void testReturnsTargetForBitmaps() {
    Bitmap bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
    Target<Bitmap> target = factory.buildTarget(view, Bitmap.class);
    target.onResourceReady(bitmap, null);
    assertThat(target).isInstanceOf(BitmapImageViewTarget.class);
  }

  @Test
  public void testReturnsTargetForBitmapDrawables() {
    BitmapDrawable drawable =
        new BitmapDrawable(
            ApplicationProvider.getApplicationContext().getResources(),
            Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_4444));

    Target<BitmapDrawable> target = factory.buildTarget(view, BitmapDrawable.class);
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
