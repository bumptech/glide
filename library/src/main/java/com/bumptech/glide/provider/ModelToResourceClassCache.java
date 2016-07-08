package com.bumptech.glide.provider;

import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import com.bumptech.glide.util.MultiClassKey;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Maintains a cache of Model + Resource class to a set of registered resource classes that are
 * subclasses of the resource class that can be decoded from the model class.
 */
public class ModelToResourceClassCache {
  private final AtomicReference<MultiClassKey> resourceClassKeyRef = new AtomicReference<>();
  private final ArrayMap<MultiClassKey, List<Class<?>>> registeredResourceClassCache =
      new ArrayMap<>();

  @Nullable
  public List<Class<?>> get(Class<?> modelClass, Class<?> resourceClass) {
    MultiClassKey key = resourceClassKeyRef.getAndSet(null);
    if (key == null) {
      key = new MultiClassKey(modelClass, resourceClass);
    } else {
      key.set(modelClass, resourceClass);
    }
    final List<Class<?>> result;
    synchronized (registeredResourceClassCache) {
       result = registeredResourceClassCache.get(key);
    }
    resourceClassKeyRef.set(key);
    return result;
  }

  public void put(Class<?> modelClass, Class<?> resourceClass, List<Class<?>> resourceClasses) {
    synchronized (registeredResourceClassCache) {
      registeredResourceClassCache
          .put(new MultiClassKey(modelClass, resourceClass), resourceClasses);
    }
  }

  public void clear() {
    synchronized (registeredResourceClassCache) {
      registeredResourceClassCache.clear();
    }
  }
}
