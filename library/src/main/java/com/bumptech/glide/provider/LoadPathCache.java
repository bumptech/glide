package com.bumptech.glide.provider;

import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import com.bumptech.glide.load.engine.LoadPath;
import com.bumptech.glide.util.MultiClassKey;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Maintains a cache of data, resource, and transcode classes to available
 * {@link com.bumptech.glide.load.engine.LoadPath}s capable of decoding with the requested types.
 */
public class LoadPathCache {
  private final ArrayMap<MultiClassKey, LoadPath<?, ?, ?>> cache = new ArrayMap<>();
  private final AtomicReference<MultiClassKey> keyRef = new AtomicReference<>();

  public boolean contains(Class<?> dataClass, Class<?> resourceClass, Class<?> transcodeClass) {
    MultiClassKey key = getKey(dataClass, resourceClass, transcodeClass);
    boolean result;
    synchronized (cache) {
      result = cache.containsKey(key);
    }
    keyRef.set(key);
    return result;
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public <Data, TResource, Transcode> LoadPath<Data, TResource, Transcode> get(
      Class<Data> dataClass, Class<TResource> resourceClass, Class<Transcode> transcodeClass) {
    MultiClassKey key = getKey(dataClass, resourceClass, transcodeClass);
    LoadPath<?, ?, ?> result;
    synchronized (cache) {
       result = cache.get(key);
    }
    keyRef.set(key);

    return (LoadPath<Data, TResource, Transcode>) result;
  }

  public void put(Class<?> dataClass, Class<?> resourceClass, Class<?> transcodeClass,
      LoadPath<?, ?, ?> loadPath) {
    synchronized (cache) {
      cache.put(new MultiClassKey(dataClass, resourceClass, transcodeClass), loadPath);
    }
  }

  private MultiClassKey getKey(Class<?> dataClass, Class<?> resourceClass,
      Class<?> transcodeClass) {
     MultiClassKey key = keyRef.getAndSet(null);
    if (key == null) {
      key = new MultiClassKey();
    }
    key.set(dataClass, resourceClass, transcodeClass);
    return key;
  }
}
