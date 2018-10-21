package com.bumptech.glide.manager;

import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import java.util.Map;

class ArchLifecycle implements Lifecycle {

  private final android.arch.lifecycle.Lifecycle lifecycle;

  private final Map<LifecycleListener, LifecycleObserver> listeners = new ArrayMap<>();

  ArchLifecycle(@NonNull android.arch.lifecycle.Lifecycle lifecycle) {
    this.lifecycle = lifecycle;
  }

  @Override
  public void addListener(@NonNull LifecycleListener listener) {
    if (listeners.containsKey(listener)) {
      return;
    }
    LifecycleObserver observer = wrap(listener);
    listeners.put(listener, observer);
    lifecycle.addObserver(observer);
  }

  @Override
  public void removeListener(@NonNull LifecycleListener listener) {
    LifecycleObserver observer = listeners.remove(listener);
    if (observer != null) {
      lifecycle.removeObserver(observer);
    }
  }

  @NonNull
  private LifecycleObserver wrap(@NonNull LifecycleListener listener) {
    try {
      Class.forName("android.arch.lifecycle.DefaultLifecycleObserver");
      return wrapWithDefaultLifecycleObserver(listener);
    } catch (ClassNotFoundException ignore) {
      return wrapWithGenericLifecycleObserver(listener);
    }
  }

  @NonNull
  private LifecycleObserver wrapWithDefaultLifecycleObserver(
      @NonNull final LifecycleListener listener) {
    return new android.arch.lifecycle.DefaultLifecycleObserver() {
      @Override
      public void onCreate(@NonNull LifecycleOwner owner) {
      }

      @Override
      public void onStart(@NonNull LifecycleOwner owner) {
        listener.onStart();
      }

      @Override
      public void onResume(@NonNull LifecycleOwner owner) {
      }

      @Override
      public void onPause(@NonNull LifecycleOwner owner) {
      }

      @Override
      public void onStop(@NonNull LifecycleOwner owner) {
        listener.onStop();
      }

      @Override
      public void onDestroy(@NonNull LifecycleOwner owner) {
        listener.onDestroy();
      }
    };
  }

  private LifecycleObserver wrapWithGenericLifecycleObserver(
      @NonNull final LifecycleListener listener) {
    return new LifecycleObserver() {
      @OnLifecycleEvent(android.arch.lifecycle.Lifecycle.Event.ON_START)
      public void onStart(@NonNull LifecycleOwner owner) {
        listener.onStart();
      }

      @OnLifecycleEvent(android.arch.lifecycle.Lifecycle.Event.ON_STOP)
      public void onStop(@NonNull LifecycleOwner owner) {
        listener.onStop();
      }

      @OnLifecycleEvent(android.arch.lifecycle.Lifecycle.Event.ON_DESTROY)
      public void onDestroy(@NonNull LifecycleOwner owner) {
        listener.onDestroy();
      }
    };
  }


}
