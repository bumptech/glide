package com.bumptech.glide.load.engine;

import android.support.annotation.NonNull;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.util.Preconditions;

/**
 * A wrapper resource that allows reference counting a wrapped {@link
 * com.bumptech.glide.load.engine.Resource} interface.
 *
 * @param <Z> The type of data returned by the wrapped {@link Resource}.
 */
class EngineResource<Z> implements Resource<Z> {
  private final boolean isCacheable;
  private final boolean isRecyclable;
  private final Resource<Z> resource;

  private ResourceListener listener;
  private Key key;
  private int acquired;
  private boolean isRecycled;

  interface ResourceListener {
    void onResourceReleased(Key key, EngineResource<?> resource);
  }

  EngineResource(Resource<Z> toWrap, boolean isCacheable, boolean isRecyclable) {
    resource = Preconditions.checkNotNull(toWrap);
    this.isCacheable = isCacheable;
    this.isRecyclable = isRecyclable;
  }

  synchronized void setResourceListener(Key key, ResourceListener listener) {
    this.key = key;
    this.listener = listener;
  }

  Resource<Z> getResource() {
    return resource;
  }

  boolean isCacheable() {
    return isCacheable;
  }

  @NonNull
  @Override
  public Class<Z> getResourceClass() {
    return resource.getResourceClass();
  }

  @NonNull
  @Override
  public Z get() {
    return resource.get();
  }

  @Override
  public int getSize() {
    return resource.getSize();
  }

  @Override
  public synchronized void recycle() {
    if (acquired > 0) {
      throw new IllegalStateException("Cannot recycle a resource while it is still acquired");
    }
    if (isRecycled) {
      throw new IllegalStateException("Cannot recycle a resource that has already been recycled");
    }
    isRecycled = true;
    if (isRecyclable) {
      resource.recycle();
    }
  }

  /**
   * Increments the number of consumers using the wrapped resource. Must be called on the main
   * thread.
   *
   * <p>This must be called with a number corresponding to the number of new consumers each time new
   * consumers begin using the wrapped resource. It is always safer to call acquire more often than
   * necessary. Generally external users should never call this method, the framework will take care
   * of this for you.
   */
  synchronized void acquire() {
    if (isRecycled) {
      throw new IllegalStateException("Cannot acquire a recycled resource");
    }
    ++acquired;
  }

  /**
   * Decrements the number of consumers using the wrapped resource. Must be called on the main
   * thread.
   *
   * <p>This must only be called when a consumer that called the {@link #acquire()} method is now
   * done with the resource. Generally external users should never call this method, the framework
   * will take care of this for you.
   */
  // listener is effectively final.
  @SuppressWarnings("SynchronizeOnNonFinalField")
  void release() {
    // To avoid deadlock, always acquire the listener lock before our lock so that the locking
    // scheme is consistent (Engine -> EngineResource). Violating this order leads to deadlock
    // (b/123646037).
    synchronized (listener) {
      synchronized (this) {
        if (acquired <= 0) {
          throw new IllegalStateException("Cannot release a recycled or not yet acquired resource");
        }
        if (--acquired == 0) {
          listener.onResourceReleased(key, this);
        }
      }
    }
  }

  @Override
  public synchronized String toString() {
    return "EngineResource{"
        + "isCacheable=" + isCacheable
        + ", listener=" + listener
        + ", key=" + key
        + ", acquired=" + acquired
        + ", isRecycled=" + isRecycled
        + ", resource=" + resource
        + '}';
  }
}
