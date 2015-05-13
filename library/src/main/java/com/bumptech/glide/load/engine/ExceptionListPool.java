package com.bumptech.glide.load.engine;

import android.support.v4.util.Pools;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple wrapper for an {@link android.support.v4.util.Pools.Pool} that returns non-null
 * {@link List}s for {@link Exception}s.
 */
public final class ExceptionListPool implements Pools.Pool<List<Exception>> {
  private static final String TAG = "ExceptionListPool";
  private final Pools.Pool<List<Exception>> pool = new Pools.SynchronizedPool<>(20);

  @Override
  public List<Exception> acquire() {
    List<Exception> result = pool.acquire();
    if (result == null) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Allocate new list");
      }
      result = new ArrayList<>();
    }
    return result;
  }

  @Override
  public boolean release(List<Exception> instance) {
    instance.clear();
    return pool.release(instance);
  }
}
