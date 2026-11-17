package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import java.util.Map;

class EngineKeyFactory {

  EngineKey buildKey(
      Object model,
      Key signature,
      int width,
      int height,
      Class<?> resourceClass,
      Class<?> transcodeClass,
      Map<Class<?>, Transformation<?>> transformations,
      Options options) {
    return new EngineKey(
        model, signature, width, height, resourceClass, transcodeClass, transformations, options);
  }
}
