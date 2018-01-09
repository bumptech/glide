package com.bumptech.glide.request.transition;

import static com.bumptech.glide.request.transition.Transition.ViewAdapter;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.animation.Animation;
import android.widget.ImageView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class ViewAnimationTest {
  private ViewTransition<Object> viewAnimation;
  private ViewAdapter adapter;
  private ImageView view;
  private ViewTransition.ViewTransitionAnimationFactory viewTransitionAnimationFactory;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    viewTransitionAnimationFactory = mock(ViewTransition.ViewTransitionAnimationFactory.class);
    view = mock(ImageView.class);
    adapter = mock(ViewAdapter.class);
    when(adapter.getView()).thenReturn(view);
    viewAnimation = new ViewTransition<>(viewTransitionAnimationFactory);
  }

  @Test
  public void testClearsAnimationOnAnimate() {
    viewAnimation.transition(null, adapter);

    verify(view).clearAnimation();
  }

  @Test
  public void testAlwaysReturnsFalse() {
    assertFalse(viewAnimation.transition(null, adapter));
  }

  @Test
  public void testStartsAnimationOnAnimate() {
    Animation animation = mock(Animation.class);
    when(viewTransitionAnimationFactory.build(any(Context.class))).thenReturn(animation);
    viewAnimation.transition(null, adapter);
    verify(view).clearAnimation();
    verify(view).startAnimation(eq(animation));
  }
}
