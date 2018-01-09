package com.bumptech.glide.request.transition;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import com.bumptech.glide.load.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ViewTransitionAnimationFactoryTest {
  private ViewTransition.ViewTransitionAnimationFactory viewTransitionAnimationFactory;
  private ViewAnimationFactory<Object> factory;

  @Before
  public void setUp() {
    viewTransitionAnimationFactory = mock(ViewTransition.ViewTransitionAnimationFactory.class);
    factory = new ViewAnimationFactory<>(viewTransitionAnimationFactory);
  }

  @Test
  public void testFactoryReturnsNoAnimationIfFromMemoryCache() {
    Transition<Object> animation =
        factory.build(DataSource.MEMORY_CACHE, true /*isFirstResource*/);
    assertEquals(NoTransition.get(), animation);
    verify(viewTransitionAnimationFactory, never()).build(RuntimeEnvironment.application);
  }

  @Test
  public void testFactoryReturnsNoAnimationIfNotFirstResource() {
    Transition<Object> animation =
        factory.build(DataSource.DATA_DISK_CACHE, false /*isFirstResource*/);
    assertEquals(NoTransition.get(), animation);
    verify(viewTransitionAnimationFactory, never()).build(RuntimeEnvironment.application);
  }

  @Test
  public void testFactoryReturnsActualAnimationIfNotIsFromMemoryCacheAndIsFirstResource() {
    Transition<Object> transition =
        factory.build(DataSource.DATA_DISK_CACHE, true /*isFirstResource*/);

    Animation animation = mock(Animation.class);
    when(viewTransitionAnimationFactory.build(any(Context.class))).thenReturn(animation);

    Transition.ViewAdapter adapter = mock(Transition.ViewAdapter.class);
    View view = mock(View.class);
    when(adapter.getView()).thenReturn(view);
    transition.transition(new Object(), adapter);

    verify(view).startAnimation(eq(animation));
  }
}
