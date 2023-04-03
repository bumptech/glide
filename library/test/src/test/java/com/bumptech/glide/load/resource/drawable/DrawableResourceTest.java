package com.bumptech.glide.load.resource.drawable;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class DrawableResourceTest {
  private TestDrawable drawable;
  private DrawableResource<TestDrawable> resource;

  @Before
  public void setUp() {
    drawable = mock(TestDrawable.class);
    resource =
        new DrawableResource<TestDrawable>(drawable) {
          @NonNull
          @Override
          public Class<TestDrawable> getResourceClass() {
            return TestDrawable.class;
          }

          @Override
          public int getSize() {
            return 0;
          }

          @Override
          public void recycle() {}
        };
  }

  @Test
  public void testDoesNotReturnOriginalDrawableOnGet() {
    when(drawable.getConstantState()).thenReturn(mock(Drawable.ConstantState.class));
    assertNotEquals(drawable, resource.get());
  }

  @SuppressWarnings("TruthIncompatibleType")
  @Test
  public void testReturnsNewDrawableOnGet() {
    GifDrawable expected = mock(GifDrawable.class);
    Drawable.ConstantState constantState = mock(Drawable.ConstantState.class);
    when(constantState.newDrawable()).thenReturn(expected);
    when(drawable.getConstantState()).thenReturn(constantState);

    assertThat(resource.get())
        .isEqualTo(/* expected: TestDrawable, actual: GifDrawable */ expected);

    verify(drawable).getConstantState();
    verify(constantState).newDrawable();
  }

  @Test
  public void get_withNullState_returnsOriginalDrawable() {
    when(drawable.getConstantState()).thenReturn(null);

    assertThat(resource.get()).isEqualTo(drawable);
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsIfDrawableIsNull() {
    new DrawableResource<TestDrawable>(null) {
      @NonNull
      @Override
      public Class<TestDrawable> getResourceClass() {
        return TestDrawable.class;
      }

      @Override
      public int getSize() {
        return 0;
      }

      @Override
      public void recycle() {}
    };
  }

  /** Just to have a type to test with which is not directly Drawable */
  private static class TestDrawable extends Drawable {
    @Override
    public void draw(@NonNull Canvas canvas) {}

    @Override
    public void setAlpha(int alpha) {}

    @Override
    public void setColorFilter(ColorFilter cf) {}

    @Override
    public int getOpacity() {
      return 0;
    }
  }
}
