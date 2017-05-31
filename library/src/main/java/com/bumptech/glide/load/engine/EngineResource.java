package com.bumptech.glide.load.engine;

import android.os.Looper;
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
  private ResourceListener listener;
  private Key key;
  private int acquired;
  private boolean isRecycled;
  private final Resource<Z> resource;

  interface ResourceListener {
    void onResourceReleased(Key key, EngineResource<?> resource);
  }

  EngineResource(Resource<Z> toWrap, boolean isCacheable) {
    resource = Preconditions.checkNotNull(toWrap);
    this.isCacheable = isCacheable;
  }

  void setResourceListener(Key key, ResourceListener listener) {
    this.key = key;
    this.listener = listener;
  }

  boolean isCacheable() {
    return isCacheable;
  }

  @Override
  public Class<Z> getResourceClass() {
    return resource.getResourceClass();
  }

  @Override
  public Z get() {
    return resource.get();
  }

  @Override
  public int getSize() {
    return resource.getSize();
  }

  @Override
  public void recycle() {
    if (acquired > 0) {
      throw new IllegalStateException("Cannot recycle a resource while it is still acquired");
    }
    if (isRecycled) {
      throw new IllegalStateException("Cannot recycle a resource that has already been recycled");
    }
    isRecycled = true;
    resource.recycle();
  }

  /**
   * Increments the number of consumers using the wrapped resource. Must be called on the main
   * thread.
   *
   * <p> This must be called with a number corresponding to the number of new consumers each time
   * new consumers begin using the wrapped resource. It is always safer to call acquire more often
   * than necessary. Generally external users should never call this method, the framework will take
   * care of this for you. </p>
   */
  void acquire() {
    if (isRecycled) {
      throw new IllegalStateException("Cannot acquire a recycled resource");
    }
    if (!Looper.getMainLooper().equals(Looper.myLooper())) {
      throw new IllegalThreadStateException("Must call acquire on the main thread");
    }
    ++acquired;
  }

  /**
   * Decrements the number of consumers using the wrapped resource. Must be called on the main
   * thread.
   *
   * <p> This must only be called when a consumer that called the {@link #acquire()} method is now
   * done with the resource. Generally external users should never callthis method, the framework
   * will take care of this for you. </p>
   */
  void release() {
    if (acquired <= 0) {
      throw new IllegalStateException("Cannot release a recycled or not yet acquired resource");
    }
    if (!Looper.getMainLooper().equals(Looper.myLooper())) {
      throw new IllegalThreadStateException("Must call release on the main thread");
    }
    if (--acquired == 0) {
      listener.onResourceReleased(key, this);
    }
  }

  @Override
  public String toString() {
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
