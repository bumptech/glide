package com.bumptech.glide.manager;

/**
 * An interface for listening to Activity/Fragment lifecycle events.
 */
public interface Lifecycle {
  /**
   * Adds the given listener to the put of listeners managed by this Lifecycle implementation.
   */
  void addListener(LifecycleListener listener);
}
