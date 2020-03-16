package com.bumptech.glide.util;
import static org.junit.Assert.assertEquals;
import androidx.annotation.Nullable;
import com.bumptech.glide.request.target.FixedSizeDrawable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class FixedSizeDrawableTest {
  private Drawable testDrawable = new Drawable() {
    @Override
    public void draw(@NonNull @android.support.annotation.NonNull Canvas canvas) {
    }
    @Override
    public void setAlpha(int alpha) {
    }
    @Override
    public void setColorFilter(
        @Nullable @android.support.annotation.Nullable ColorFilter colorFilter) {
    }
    @Override
    public int getOpacity() {
      return 0;
    }
  };
  @Test
  public void testPositiveConfiguration(){
    FixedSizeDrawable testFixSizedDrawable = new FixedSizeDrawable(testDrawable, 10, 10);
    testFixSizedDrawable.setChangingConfigurations(1);
    assertEquals(1, testFixSizedDrawable.getChangingConfigurations());
  }
  @Test
  public void testNegativeConfiguration(){
    FixedSizeDrawable testFixSizedDrawable = new FixedSizeDrawable(testDrawable, 10, 10);
    testFixSizedDrawable.setChangingConfigurations(-1);
    assertEquals(-1, testFixSizedDrawable.getChangingConfigurations());
  }
}