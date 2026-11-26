package com.bumptech.glide.load.engine;

import androidx.collection.ArrayMap;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.util.EqualityKeyProxy;
import com.bumptech.glide.util.Preconditions;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

class EngineKeyFactory {

  EngineKey buildKey(
      Object model,
      Key signature,
      int width,
      int height,
      Class<?> resourceClass,
      Class<?> transcodeClass,
      Map<Class<?>, Transformation<?>> transformations,
      Options options,
      boolean allowWeakReferences) {
    return new EngineKey(
        EqualityKeyProxy.of(model, allowWeakReferences),
        signature,
        width,
        height,
        resourceClass,
        transcodeClass,
        toEqualityKeyProxyMap(transformations, allowWeakReferences),
        options);
  }

  private static Map<Class<?>, EqualityKeyProxy<Transformation<?>>> toEqualityKeyProxyMap(
      Map<Class<?>, Transformation<?>> transformations, boolean allowWeakReferences) {
    Preconditions.checkNotNull(transformations);
    if (transformations.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Class<?>, EqualityKeyProxy<Transformation<?>>> result =
        new ArrayMap<>(transformations.size());
    for (Entry<Class<?>, Transformation<?>> entry : transformations.entrySet()) {
      result.put(entry.getKey(), EqualityKeyProxy.of(entry.getValue(), allowWeakReferences));
    }
    return result;
  }
}
