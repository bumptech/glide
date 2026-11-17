package com.bumptech.glide.load.engine;

import androidx.annotation.NonNull;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.util.EqualityKeyProxy;
import com.bumptech.glide.util.Preconditions;
import java.security.MessageDigest;
import java.util.Map;

/** An in memory only cache key used to multiplex loads. */
class EngineKey implements Key {
  private final EqualityKeyProxy<Object> model;
  private final Key signature;
  private final int width;
  private final int height;
  private final Class<?> resourceClass;
  private final Class<?> transcodeClass;
  private final Map<Class<?>, EqualityKeyProxy<Transformation<?>>> transformations;
  private final Options options;
  private int hashCode;

  EngineKey(
      EqualityKeyProxy<Object> model,
      Key signature,
      int width,
      int height,
      Class<?> resourceClass,
      Class<?> transcodeClass,
      Map<Class<?>, EqualityKeyProxy<Transformation<?>>> transformations,
      Options options) {
    this.model = Preconditions.checkNotNull(model);
    this.signature = Preconditions.checkNotNull(signature, "Signature must not be null");
    this.width = width;
    this.height = height;
    this.resourceClass =
        Preconditions.checkNotNull(resourceClass, "Resource class must not be null");
    this.transcodeClass =
        Preconditions.checkNotNull(transcodeClass, "Transcode class must not be null");
    this.transformations = Preconditions.checkNotNull(transformations);
    this.options = Preconditions.checkNotNull(options);
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
        + "model="
        + model
        + ", width="
        + width
        + ", height="
        + height
        + ", resourceClass="
        + resourceClass
        + ", transcodeClass="
        + transcodeClass
        + ", signature="
        + signature
        + ", hashCode="
        + hashCode
        + ", transformations="
        + transformations
        + ", options="
        + options
        + '}';
  }

  @Override
  public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    throw new UnsupportedOperationException();
  }
}
