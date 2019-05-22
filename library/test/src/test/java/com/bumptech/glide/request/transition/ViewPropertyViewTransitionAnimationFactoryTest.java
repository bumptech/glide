package com.bumptech.glide.request.transition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;

import com.bumptech.glide.load.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ViewPropertyViewTransitionAnimationFactoryTest {

  private ViewPropertyAnimationFactory<Object> factory;

  @Before
  public void setUp() {
    ViewPropertyTransition.Animator animator = mock(ViewPropertyTransition.Animator.class);
    factory = new ViewPropertyAnimationFactory<>(animator);
  }

  @Test
  public void testReturnsNoAnimationIfFromMemoryCache() {
    assertEquals(
        NoTransition.get(), factory.build(DataSource.MEMORY_CACHE, true /*isFirstResource*/));
  }

  @Test
  public void testReturnsNoAnimationIfNotFirstResource() {
    assertEquals(
        NoTransition.get(), factory.build(DataSource.DATA_DISK_CACHE, false /*isFirstResource*/));
  }

  @Test
  public void testReturnsAnimationIfNotFromMemoryCacheAndFirstResource() {
    assertNotEquals(
        NoTransition.get(), factory.build(DataSource.DATA_DISK_CACHE, true /*isFirstResource*/));
  }
}
