package com.bumptech.glide.manager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class LifecycleTest {

  private ActivityFragmentLifecycle lifecycle;
  private LifecycleListener listener;

  @Before
  public void setUp() {
    lifecycle = new ActivityFragmentLifecycle();
    listener = mock(LifecycleListener.class);
  }

  @Test
  public void testNotifiesAddedListenerOnStart() {
    lifecycle.addListener(listener);
    lifecycle.onStart();
    verify(listener).onStart();
  }

  @Test
  public void testNotifiesAddedListenerOfStartIfStarted() {
    lifecycle.onStart();
    lifecycle.addListener(listener);
    verify(listener).onStart();
  }

  @Test
  public void testDoesNotNotifyAddedListenerOfStartIfDestroyed() {
    lifecycle.onStart();
    lifecycle.onStop();
    lifecycle.onDestroy();
    lifecycle.addListener(listener);

    verify(listener, never()).onStart();
  }

  @Test
  public void testDoesNotNotifyListenerOfStartIfStartedThenStopped() {
    lifecycle.onStart();
    lifecycle.onStop();
    lifecycle.addListener(listener);
    verify(listener, never()).onStart();
  }

  @Test
  public void testNotifiesAddedListenerOnStop() {
    lifecycle.onStart();
    lifecycle.addListener(listener);
    lifecycle.onStop();
    verify(listener).onStop();
  }

  @Test
  public void testNotifiesAddedListenerOfStopIfStopped() {
    lifecycle.onStop();
    lifecycle.addListener(listener);
    verify(listener).onStop();
  }

  @Test
  public void testDoesNotNotifyAddedListenerOfStopIfDestroyed() {
    lifecycle.onStart();
    lifecycle.onStop();
    lifecycle.onDestroy();
    lifecycle.addListener(listener);
    verify(listener, never()).onStop();
  }

  @Test
  public void testDoesNotNotifyListenerOfStopIfStoppedThenStarted() {
    lifecycle.onStop();
    lifecycle.onStart();
    lifecycle.addListener(listener);
    verify(listener, never()).onStop();
  }

  @Test
  public void testNotifiesAddedListenerOnDestroy() {
    lifecycle.addListener(listener);
    lifecycle.onDestroy();
    verify(listener).onDestroy();
  }

  @Test
  public void testNotifiesAddedListenerOfDestroyIfDestroyed() {
    lifecycle.onDestroy();
    lifecycle.addListener(listener);
    verify(listener).onDestroy();
  }

  @Test
  public void testNotifiesMultipleListeners() {
    lifecycle.onStart();
    int toNotify = 20;
    List<LifecycleListener> listeners = new ArrayList<>();
    for (int i = 0; i < toNotify; i++) {
      listeners.add(mock(LifecycleListener.class));
    }
    for (LifecycleListener lifecycleListener : listeners) {
      lifecycle.addListener(lifecycleListener);
    }
    lifecycle.onStop();
    lifecycle.onDestroy();
    for (LifecycleListener lifecycleListener : listeners) {
      verify(lifecycleListener).onStart();
      verify(lifecycleListener).onStop();
      verify(lifecycleListener).onDestroy();
    }
  }
}
