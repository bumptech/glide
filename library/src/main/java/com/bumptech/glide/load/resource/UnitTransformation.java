package com.bumptech.glide.load.resource;

import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import java.security.MessageDigest;

/**
 * A noop Transformation that simply returns the given resource.
 *
 * @param <T> The type of the resource that will always be returned unmodified.
 */
public final class UnitTransformation<T> implements Transformation<T> {
  private static final Transformation<?> TRANSFORMATION = new UnitTransformation<Object>();

  /**
   * Returns a UnitTransformation for the given type.
   *
   * @param <T> The type of the resource to be transformed.
   */
  @SuppressWarnings("unchecked")
  public static <T> UnitTransformation<T> get() {
    return (UnitTransformation<T>) TRANSFORMATION;
  }

  private UnitTransformation() {
    // Only accessible as a singleton.
  }

  @Override
  public Resource<T> transform(Resource<T> resource, int outWidth, int outHeight) {
    return resource;
  }

  @Override
  public void updateDiskCacheKey(MessageDigest messageDigest) {
    // Do nothing.
  }

  /* Use default implementations of equals and hashcode. */
}
