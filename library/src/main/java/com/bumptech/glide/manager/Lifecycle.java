package com.bumptech.glide.manager;

import androidx.annotation.NonNull;

/** An interface for listening to Activity/Fragment lifecycle events. */
public interface Lifecycle {
  /** Adds the given listener to the set of listeners managed by this Lifecycle implementation. */
  void addListener(@NonNull LifecycleListener listener);

  /**
   * Removes the given listener from the set of listeners managed by this Lifecycle implementation,
   * returning {@code true} if the listener was removed successfully, and {@code false} otherwise.
   *
   * <p>This is an optimization only, there is no guarantee that every added listener will
   * eventually be removed.
   */
  void removeListener(@NonNull LifecycleListener listener);
}
