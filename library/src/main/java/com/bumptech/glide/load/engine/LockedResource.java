package com.bumptech.glide.load.engine;

import android.support.v4.util.Pools;
import com.bumptech.glide.util.Synthetic;
import com.bumptech.glide.util.pool.FactoryPools;
import com.bumptech.glide.util.pool.StateVerifier;

/**
 * A resource that defers any calls to {@link Resource#recycle()} until after {@link #unlock()} is
 * called.
 *
 * <p>If the resource was recycled prior to {@link #unlock()}, then {@link #unlock()} will also
 * recycle the resource.
 */
final class LockedResource<Z> implements Resource<Z>,
    FactoryPools.Poolable {
  private static final Pools.Pool<LockedResource<?>> POOL = FactoryPools.threadSafe(20,
      new FactoryPools.Factory<LockedResource<?>>() {
        @Override
        public LockedResource<?> create() {
          return new LockedResource<Object>();
        }
      });
  private final StateVerifier stateVerifier = StateVerifier.newInstance();
  private Resource<Z> toWrap;
  private boolean isLocked;
  private boolean isRecycled;

  @SuppressWarnings("unchecked")
  static <Z> LockedResource<Z> obtain(Resource<Z> resource) {
    LockedResource<Z> result = (LockedResource<Z>) POOL.acquire();
    result.init(resource);
    return result;
  }

  @Synthetic
  LockedResource() { }

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
    stateVerifier.throwIfRecycled();

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
    stateVerifier.throwIfRecycled();

    this.isRecycled = true;
    if (!isLocked) {
      toWrap.recycle();
      release();
    }
  }

  @Override
  public StateVerifier getVerifier() {
    return stateVerifier;
  }
}
