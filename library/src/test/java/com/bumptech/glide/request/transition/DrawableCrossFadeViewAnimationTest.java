package com.bumptech.glide.request.transition;

import static com.bumptech.glide.request.transition.Transition.ViewAdapter;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class DrawableCrossFadeViewAnimationTest {
  private CrossFadeHarness harness;

  @Before
  public void setup() {
    harness = new CrossFadeHarness();
  }

  @Test
  public void testStartsDefaultAnimationIfNoPreviousDrawableIsNotSet() {
    when(harness.adapter.getView()).thenReturn(harness.view);
    harness.animation.transition(harness.current, harness.adapter);
    verify(harness.defaultAnimation).transition(eq(harness.current), eq(harness.adapter));
  }

  @Test
  public void testIgnoresNullViews() {
    when(harness.adapter.getView()).thenReturn(null);
    harness.animation.transition(harness.current, harness.adapter);
  }

  @Test
  public void testReturnsFalseIfStartsDefaultAnimation() {
    assertFalse(harness.animation.transition(harness.current, harness.adapter));
  }

  @Test
  public void testSetsTransitionDrawableIfPreviousIsNotNull() {
    Drawable previous = new ColorDrawable(Color.WHITE);
    when(harness.adapter.getCurrentDrawable()).thenReturn(previous);
    harness.animation.transition(harness.current, harness.adapter);

    verify(harness.adapter).setDrawable(any(TransitionDrawable.class));
  }

  @Test
  public void testReturnsTrueIfSetsTransitionDrawable() {
    Drawable previous = new ColorDrawable(Color.RED);
    when(harness.adapter.getCurrentDrawable()).thenReturn(previous);
    assertTrue(harness.animation.transition(harness.current, harness.adapter));
  }

  @SuppressWarnings("unchecked")
  private static class CrossFadeHarness {
    Drawable current = new ColorDrawable(Color.GRAY);
    ViewAdapter adapter = mock(ViewAdapter.class);
    ImageView view = mock(ImageView.class);
    Transition<Drawable> defaultAnimation = mock(Transition.class);
    int duration = 200;
    DrawableCrossFadeTransition animation =
        new DrawableCrossFadeTransition(defaultAnimation, duration, true /*isCrossFadeEnabled*/);
  }
}

