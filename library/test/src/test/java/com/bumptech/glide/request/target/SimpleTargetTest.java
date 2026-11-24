package com.bumptech.glide.request.target;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.request.transition.Transition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SimpleTargetTest {

  @Test
  public void throwsOnGetSizeIfGivenWidthIsLessThanZero() {
    SimpleTarget<Object> target = getTarget(-1, 1);
    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    assertThrows(IllegalArgumentException.class, () -> target.getSize(cb));
  }

  @Test
  public void throwsOnGetSizeIfGivenWidthIsEqualToZero() {
    SimpleTarget<Object> target = getTarget(0, 1);
    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    assertThrows(IllegalArgumentException.class, () -> target.getSize(cb));
  }

  @Test
  public void throwsOnGetSizeIfGivenHeightIsLessThanZero() {
    SimpleTarget<Object> target = getTarget(1, -1);
    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    assertThrows(IllegalArgumentException.class, () -> target.getSize(cb));
  }

  @Test
  public void throwsOnGetSizeIfGivenHeightIsEqualToZero() {
    SimpleTarget<Object> target = getTarget(1, 0);
    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    assertThrows(IllegalArgumentException.class, () -> target.getSize(cb));
  }

  @Test
  public void canBeConstructedWithoutDimensions() {
    new SimpleTarget<Object>() {
      @Override
      public void onResourceReady(
          @NonNull Object resource, @Nullable Transition<? super Object> transition) {
        // Do nothing.
      }
    };
  }

  @Test
  public void constructorDoesNotThrowWithSizeOriginal() {
    getTarget(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
  }

  @Test
  public void getSizeDoesNotThrowWithSizeOriginal() {
    getTarget(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).getSize(mock(SizeReadyCallback.class));
  }

  private SimpleTarget<Object> getTarget(int width, int height) {
    return new SimpleTarget<Object>(width, height) {
      @Override
      public void onResourceReady(
          @NonNull Object resource, @Nullable Transition<? super Object> transition) {
        // Do nothing.
      }
    };
  }
}
