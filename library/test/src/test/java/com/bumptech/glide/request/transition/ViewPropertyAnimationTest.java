package com.bumptech.glide.request.transition;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.view.View;
import android.widget.ImageView;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.request.transition.Transition.ViewAdapter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
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
    ImageView view = new ImageView(ApplicationProvider.getApplicationContext());
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
