package com.bumptech.glide.request.transition;

import static com.bumptech.glide.request.transition.Transition.ViewAdapter;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.view.View;
import android.widget.ImageView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class ViewPropertyAnimationTest {
  private ViewPropertyTransition.Animator animator;
  private ViewPropertyTransition<Object> animation;

  @Before
  public void setUp() {
    animator = mock(ViewPropertyTransition.Animator.class);
    animation = new ViewPropertyTransition<>(animator);
  }

  @Test
  public void testAlwaysReturnsFalse() {
    assertFalse(animation.transition(new Object(), mock(ViewAdapter.class)));
  }

  @Test
  public void testCallsAnimatorWithGivenView() {
    ImageView view = new ImageView(RuntimeEnvironment.application);
    ViewAdapter adapter = mock(ViewAdapter.class);
    when(adapter.getView()).thenReturn(view);
    animation.transition(new Object(), adapter);

    verify(animator).animate(eq(view));
  }

  @Test
  public void testDoesNotCallAnimatorIfGivenAdapterWithNullView() {
    ViewAdapter adapter = mock(ViewAdapter.class);
    animation.transition(new Object(), adapter);

    verify(animator, never()).animate(any(View.class));
  }
}
