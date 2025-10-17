package com.bumptech.glide.manager;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import com.bumptech.glide.util.Util;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("OnLifecycleEvent") // Glide doesn't support Java 8
final class LifecycleLifecycle implements Lifecycle, LifecycleObserver {
  @NonNull
  private final Set<LifecycleListener> lifecycleListeners = new HashSet<LifecycleListener>();

  @NonNull private final androidx.lifecycle.Lifecycle lifecycle;

  LifecycleLifecycle(androidx.lifecycle.Lifecycle lifecycle) {
    this.lifecycle = lifecycle;
    lifecycle.addObserver(this);
  }

  @OnLifecycleEvent(Event.ON_START)
  public void onStart(@NonNull LifecycleOwner owner) {
    for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
      lifecycleListener.onStart();
    }
  }

  @OnLifecycleEvent(Event.ON_STOP)
  public void onStop(@NonNull LifecycleOwner owner) {
    for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
      lifecycleListener.onStop();
    }
  }

  @OnLifecycleEvent(Event.ON_DESTROY)
  public void onDestroy(@NonNull LifecycleOwner owner) {
    for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
      lifecycleListener.onDestroy();
    }
    owner.getLifecycle().removeObserver(this);
  }

  @Override
  public void addListener(@NonNull LifecycleListener listener) {
    lifecycleListeners.add(listener);

    if (lifecycle.getCurrentState() == State.DESTROYED) {
      listener.onDestroy();
    } else if (lifecycle.getCurrentState().isAtLeast(State.STARTED)) {
      listener.onStart();
    } else {
      listener.onStop();
    }
  }

  @Override
  public void removeListener(@NonNull LifecycleListener listener) {
    lifecycleListeners.remove(listener);
  }
}
