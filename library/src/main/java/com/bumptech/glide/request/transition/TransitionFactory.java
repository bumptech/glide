package com.bumptech.glide.request.transition;

/**
 * A factory class that can produce different {@link Transition}s based on the state of the
 * request.
 *
 * @param <R> The type of resource that needs to be animated into the target.
 */
public interface TransitionFactory<R> {

  /**
   * Returns a new {@link Transition}.
   *
   * @param isFromMemoryCache True if this will be a transition for a resource that was loaded from
   *                          the memory cache.
   * @param isFirstResource   True if this is the first resource to be loaded into the target.
   */
  Transition<R> build(boolean isFromMemoryCache, boolean isFirstResource);
}
