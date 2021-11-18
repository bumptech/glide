package com.bumptech.glide.manager;

import androidx.annotation.NonNull;
import com.bumptech.glide.util.Util;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A {@link com.bumptech.glide.manager.Lifecycle} implementation for tracking and notifying
 * listeners of {@link android.app.Fragment} and {@link android.app.Activity} lifecycle events.
 */
class ActivityFragmentLifecycle implements Lifecycle {
  private final Set<LifecycleListener> lifecycleListeners =
      Collections.newSetFromMap(new WeakHashMap<LifecycleListener, Boolean>());
  private boolean isStarted;
  private boolean isDestroyed;

  /**
   * Adds the given listener to the list of listeners to be notified on each lifecycle event.
   *
   * <p>The latest lifecycle event will be called on the given listener synchronously in this
   * method. If the activity or fragment is stopped, {@link LifecycleListener#onStop()}} will be
   * called, and same for onStart and onDestroy.
   *
   * <p>Note - {@link com.bumptech.glide.manager.LifecycleListener}s that are added more than once
   * will have their lifecycle methods called more than once. It is the caller's responsibility to
   * avoid adding listeners multiple times.
   */
  @Override
  public void addListener(@NonNull LifecycleListener listener) {
    lifecycleListeners.add(listener);

    if (isDestroyed) {
      listener.onDestroy();
    } else if (isStarted) {
      listener.onStart();
    } else {
      listener.onStop();
    }
  }

  @Override
  public void removeListener(@NonNull LifecycleListener listener) {
    lifecycleListeners.remove(listener);
  }
  // TODO Glide生命周期变化时调用-onStart()
  void onStart() {
    isStarted = true;
    for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
      //这里跳转到RequestManager的onStart()方法
      lifecycleListener.onStart();
    }
  }
  // TODO Glide生命周期变化时调用-onStop()
  void onStop() {
    isStarted = false;
    for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
      //这里跳转到RequestManager的 onStop()方法
      lifecycleListener.onStop();
    }
  }
  // TODO Glide生命周期变化时调用-onDestroy()
  void onDestroy() {
    isDestroyed = true;
    for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
      //这里跳转到RequestManager的 onDestroy()方法
      lifecycleListener.onDestroy();
    }
  }
}
