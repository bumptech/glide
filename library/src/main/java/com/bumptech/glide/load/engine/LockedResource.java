package com.bumptech.glide.load.engine;

import android.support.v4.util.Pools;

/**
 * A resource that defers any calls to {@link Resource#recycle()} until after {@link #unlock()} is
 * called.
 *
 * <p>If the resource was recycled prior to {@link #unlock()}, then {@link #unlock()} will also
 * recycle the resource.
 */
final class LockedResource<Z> implements Resource<Z> {
  private static final Pools.Pool<LockedResource<?>> POOL = new Pools.SynchronizedPool<>(20);
  private Resource<Z> toWrap;
  private boolean isLocked;
  private boolean isRecycled;

  @SuppressWarnings("unchecked")
  static <Z> LockedResource<Z> obtain(Resource<Z> resource) {
    LockedResource<Z> result = (LockedResource<Z>) POOL.acquire();
    if (result == null) {
      result = new LockedResource<>();
    }
    result.init(resource);
    return result;
  }

  private LockedResource() { }

  private void init(Resource<Z> toWrap) {
    isRecycled = false;
    isLocked = true;
    this.toWrap = toWrap;
  }

  private void release() {
    toWrap = null;
    POOL.release(this);
  }

  public synchronized void unlock() {
    if (!isLocked) {
      throw new IllegalStateException("Already unlocked");
    }
    this.isLocked = false;
    if (isRecycled) {
      recycle();
    }
  }

  @Override
  public Class<Z> getResourceClass() {
    return toWrap.getResourceClass();
  }

  @Override
  public Z get() {
    return toWrap.get();
  }

  @Override
  public int getSize() {
    return toWrap.getSize();
  }

  @Override
  public synchronized void recycle() {
    this.isRecycled = true;
    if (!isLocked) {
      toWrap.recycle();
      release();
    }
  }
}
