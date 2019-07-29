package com.bumptech.glide.request.transition;

import static com.bumptech.glide.request.transition.Transition.ViewAdapter;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class DrawableCrossFadeViewAnimationTest {
  private CrossFadeHarness harness;

  @Before
  public void setup() {
    harness = new CrossFadeHarness();
  }

  @Test
  public void testIgnoresNullViews() {
    when(harness.adapter.getView()).thenReturn(null);
    harness.animation.transition(harness.current, harness.adapter);
  }

  @Test
  public void transition_withNonNullPreviousDrawable_setsTransitionDrawable() {
    Drawable previous = new ColorDrawable(Color.WHITE);
    when(harness.adapter.getCurrentDrawable()).thenReturn(previous);
    harness.animation.transition(harness.current, harness.adapter);

    verify(harness.adapter).setDrawable(any(TransitionDrawable.class));
  }

  @Test
  public void transition_withNullPreviousDrawable_setsTransitionDrawable() {
    harness.animation.transition(harness.current, harness.adapter);

    verify(harness.adapter).setDrawable(any(TransitionDrawable.class));
  }

  @Test
  public void transition_withNoCurrentDrawable_returnsTrue() {
    assertTrue(harness.animation.transition(harness.current, harness.adapter));
  }

  @Test
  public void transition_withCurrentDrawable_returnsTrue() {
    Drawable previous = new ColorDrawable(Color.RED);
    when(harness.adapter.getCurrentDrawable()).thenReturn(previous);
    assertTrue(harness.animation.transition(harness.current, harness.adapter));
  }

  @SuppressWarnings("unchecked")
  private static class CrossFadeHarness {
    final Drawable current = new ColorDrawable(Color.GRAY);
    final ViewAdapter adapter = mock(ViewAdapter.class);
    final int duration = 200;
    final DrawableCrossFadeTransition animation =
        new DrawableCrossFadeTransition(duration, true /*isCrossFadeEnabled*/);
  }
}
