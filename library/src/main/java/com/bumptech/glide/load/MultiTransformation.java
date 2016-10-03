package com.bumptech.glide.load;

import com.bumptech.glide.load.engine.Resource;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;

/**
 * A transformation that applies one or more transformations in iteration order to a resource.
 *
 * @param <T> The type of {@link com.bumptech.glide.load.engine.Resource} that will be transformed.
 */
public class MultiTransformation<T> implements Transformation<T> {
  private final Collection<? extends Transformation<T>> transformations;

  @SafeVarargs
  public MultiTransformation(Transformation<T>... transformations) {
    if (transformations.length < 1) {
      throw new IllegalArgumentException(
          "MultiTransformation must contain at least one Transformation");
    }
    this.transformations = Arrays.asList(transformations);
  }

  public MultiTransformation(Collection<? extends Transformation<T>> transformationList) {
    if (transformationList.isEmpty()) {
      throw new IllegalArgumentException(
          "MultiTransformation must contain at least one Transformation");
    }
    this.transformations = transformationList;
  }

  @Override
  public Resource<T> transform(Resource<T> resource, int outWidth, int outHeight) {
    Resource<T> previous = resource;

    for (Transformation<T> transformation : transformations) {
      Resource<T> transformed = transformation.transform(previous, outWidth, outHeight);
      if (previous != null && !previous.equals(resource) && !previous.equals(transformed)) {
        previous.recycle();
      }
      previous = transformed;
    }
    return previous;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof MultiTransformation) {
      MultiTransformation<?> other = (MultiTransformation<?>) o;
      return transformations.equals(other.transformations);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return transformations.hashCode();
  }

  @Override
  public void updateDiskCacheKey(MessageDigest messageDigest) {
    for (Transformation<T> transformation : transformations) {
      transformation.updateDiskCacheKey(messageDigest);
    }
  }
}
