package com.bumptech.glide.request.target;

import static org.mockito.Mockito.mock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.request.transition.Transition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SimpleTargetTest {

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsOnGetSizeIfGivenWidthIsLessThanZero() {
    getTarget(-1, 1).getSize(mock(SizeReadyCallback.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsOnGetSizeIfGivenWidthIsEqualToZero() {
    getTarget(0, 1).getSize(mock(SizeReadyCallback.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsOnGetSizeIfGivenHeightIsLessThanZero() {
    getTarget(1, -1).getSize(mock(SizeReadyCallback.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsOnGetSizeIfGivenHeightIsEqualToZero() {
    getTarget(1, 0).getSize(mock(SizeReadyCallback.class));
  }

  @Test
  public void testCanBeConstructedWithoutDimensions() {
    new SimpleTarget<Object>() {
      @Override
      public void onResourceReady(
          @NonNull Object resource, @Nullable Transition<? super Object> transition) {
        // Do nothing.
      }
    };
  }

  @Test
  public void testConstructorDoesNotThrowWithSizeOriginal() {
    getTarget(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
  }

  @Test
  public void testGetSizeDoesNotThrowWithSizeOriginal() {
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
