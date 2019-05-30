package com.bumptech.glide.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
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
  public List<Class<?>> get(
      @NonNull Class<?> modelClass,
      @NonNull Class<?> resourceClass,
      @NonNull Class<?> transcodeClass) {
    MultiClassKey key = resourceClassKeyRef.getAndSet(null);
    if (key == null) {
      key = new MultiClassKey(modelClass, resourceClass, transcodeClass);
    } else {
      key.set(modelClass, resourceClass, transcodeClass);
    }
    final List<Class<?>> result;
    synchronized (registeredResourceClassCache) {
      result = registeredResourceClassCache.get(key);
    }
    resourceClassKeyRef.set(key);
    return result;
  }

  public void put(
      @NonNull Class<?> modelClass,
      @NonNull Class<?> resourceClass,
      @NonNull Class<?> transcodeClass,
      @NonNull List<Class<?>> resourceClasses) {
    synchronized (registeredResourceClassCache) {
      registeredResourceClassCache.put(
          new MultiClassKey(modelClass, resourceClass, transcodeClass), resourceClasses);
    }
  }

  public void clear() {
    synchronized (registeredResourceClassCache) {
      registeredResourceClassCache.clear();
    }
  }
}
