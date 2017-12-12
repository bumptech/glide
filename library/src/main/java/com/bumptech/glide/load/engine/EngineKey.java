package com.bumptech.glide.load.engine;

import android.support.annotation.NonNull;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import java.security.MessageDigest;
import java.util.Map;

/**
 * An in memory only cache key used to multiplex loads.
 */
class EngineKey implements Key {
  private final Object model;
  private final int width;
  private final int height;
  private final Class<?> resourceClass;
  private final Class<?> transcodeClass;
  private final Key signature;
  private final Map<Class<?>, Transformation<?>> transformations;
  private final Options options;
  private int hashCode;

  EngineKey(
      @NonNull Object model,
      @NonNull Key signature,
      int width,
      int height,
      @NonNull Map<Class<?>, Transformation<?>> transformations,
      @NonNull Class<?> resourceClass,
      @NonNull Class<?> transcodeClass,
      @NonNull Options options) {
    this.model = model;
    this.signature = signature;
    this.width = width;
    this.height = height;
    this.transformations = transformations;
    this.resourceClass = resourceClass;
    this.transcodeClass = transcodeClass;
    this.options = options;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof EngineKey) {
      EngineKey other = (EngineKey) o;
      return model.equals(other.model)
          && signature.equals(other.signature)
          && height == other.height
          && width == other.width
          && transformations.equals(other.transformations)
          && resourceClass.equals(other.resourceClass)
          && transcodeClass.equals(other.transcodeClass)
          && options.equals(other.options);
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      hashCode = model.hashCode();
      hashCode = 31 * hashCode + signature.hashCode();
      hashCode = 31 * hashCode + width;
      hashCode = 31 * hashCode + height;
      hashCode = 31 * hashCode + transformations.hashCode();
      hashCode = 31 * hashCode + resourceClass.hashCode();
      hashCode = 31 * hashCode + transcodeClass.hashCode();
      hashCode = 31 * hashCode + options.hashCode();
    }
    return hashCode;
  }

  @Override
  public String toString() {
    return "EngineKey{"
        + "model=" + model
        + ", width=" + width
        + ", height=" + height
        + ", resourceClass=" + resourceClass
        + ", transcodeClass=" + transcodeClass
        + ", signature=" + signature
        + ", hashCode=" + hashCode
        + ", transformations=" + transformations
        + ", options=" + options
        + '}';
  }

  @Override
  public void updateDiskCacheKey(MessageDigest messageDigest) {
    throw new UnsupportedOperationException();
  }
}
