package com.bumptech.glide.load.resource.drawable;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class DrawableResourceTest {
  private TestDrawable drawable;
  private DrawableResource<TestDrawable> resource;
  private Resources resources;

  @Before
  public void setUp() {
    resources = RuntimeEnvironment.application.getResources();
    drawable = mock(TestDrawable.class);
    resource = new DrawableResource<TestDrawable>(drawable) {
      @Override
      public Class<TestDrawable> getResourceClass() {
        return TestDrawable.class;
      }

      @Override
      public int getSize() {
        return 0;
      }

      @Override
      public void recycle() {
      }
    };
  }

  @Test
  public void testDoesNotReturnOriginalDrawableOnGet() {
    when(drawable.getConstantState()).thenReturn(mock(Drawable.ConstantState.class));
    assertNotEquals(drawable, resource.get());
  }

  @Test
  public void testReturnsNewDrawableOnGet() {
    Drawable expected =
        new BitmapDrawable(resources, Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
    Drawable.ConstantState constantState = mock(Drawable.ConstantState.class);
    when(constantState.newDrawable()).thenReturn(expected);
    when(drawable.getConstantState()).thenReturn(constantState);

    assertThat(resource.get()).isEqualTo(expected);

    verify(drawable).getConstantState();
    verify(constantState).newDrawable();
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsIfDrawableIsNull() {
    new DrawableResource<TestDrawable>(null) {
      @Override
      public Class<TestDrawable> getResourceClass() {
        return TestDrawable.class;
      }

      @Override
      public int getSize() {
        return 0;
      }

      @Override
      public void recycle() {

      }
    };
  }

  /**
   * Just to have a type to test with which is not directly Drawable
   */
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
