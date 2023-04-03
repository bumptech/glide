package com.bumptech.glide.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNull;

import android.view.View;
import android.view.ViewGroup;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class ViewPreloadSizeProviderTest {

  private View view;
  private ViewPreloadSizeProvider<Object> provider;

  @Before
  public void setUp() {
    view = new View(ApplicationProvider.getApplicationContext());
    provider = new ViewPreloadSizeProvider<>();
  }

  @Test
  public void testReturnsNullFromGetPreloadSizeBeforeHasSize() {
    assertNull(provider.getPreloadSize(new Object(), 0, 0));
  }

  @Test
  public void testReturnsValidSizeFromGetPreloadSizeAfterHasSize() {
    int width = 4123;
    int height = 342;
    provider.onSizeReady(width, height);

    int[] size = provider.getPreloadSize(new Object(), 0, 0);
    assertThat(size).asList().containsExactly(width, height).inOrder();
  }

  @Test
  public void testDoesNotObtainSizeFromViewOnceSizeIsSet() {
    int width = 123;
    int height = 456;
    provider.onSizeReady(width, height);
    view.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
    view.layout(0, 0, 1, 1);

    provider.setView(view);

    int[] size = provider.getPreloadSize(new Object(), 0, 0);
    assertThat(size).asList().containsExactly(width, height).inOrder();
  }

  @Test
  public void testCanObtainFixedSizeFromView() {
    int width = 123;
    int height = 456;
    view.setLayoutParams(new ViewGroup.LayoutParams(width, height));
    view.layout(0, 0, width, height);

    provider.setView(view);

    int[] size = provider.getPreloadSize(new Object(), 0, 0);
    assertThat(size).asList().containsExactly(width, height).inOrder();
  }

  @Test
  public void testIgnoresNewViewIfAlreadyWaitingOnSizeOfAnotherView() {
    provider.setView(view);

    View newView = new View(ApplicationProvider.getApplicationContext());
    newView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
    provider.setView(newView);

    assertNull(provider.getPreloadSize(new Object(), 0, 0));
  }

  @Test
  public void testCanObtainSizeFromViewWhenGivenViewInConstructor() {
    int width = 100;
    int height = 200;
    view.setLayoutParams(new ViewGroup.LayoutParams(width, height));
    view.layout(0, 0, width, height);

    provider = new ViewPreloadSizeProvider<>(view);

    int[] size = provider.getPreloadSize(new Object(), 0, 0);
    assertThat(size).asList().containsExactly(width, height).inOrder();
  }
}
