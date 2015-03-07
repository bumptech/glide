package com.bumptech.glide.load;

import com.bumptech.glide.load.engine.Resource;

/**
 * A class for performing an arbitrary transformation on a resource that implements
 * {@link #equals(Object)} and {@link #hashCode()}} to identify the transformation in the memory
 * cache and {@link #updateDiskCacheKey(java.security.MessageDigest)}} to identify the
 * transformation in disk caches.
 *
 * <p> Using the fully qualified class name (not {@link Class#getName()} to avoid proguard
 * obfuscation) is an easy way to implement
 * {@link #updateDiskCacheKey(java.security.MessageDigest)}} correctly. </p>
 *
 * @param <T> The type of the resource being transformed.
 */
public interface Transformation<T> extends Key {

  /**
   * Transforms the given resource and returns the transformed resource.
   *
   * <p> Note - If the original resource object is not returned, the original resource will be
   * recycled and it's internal resources may be reused. This means it is not safe to rely on the
   * original resource or any internal state of the original resource in any new resource that is
   * created. Usually this shouldn't occur, but if absolutely necessary either the original resource
   * object can be returned with modified internal state, or the data in the original resource can
   * be copied into the transformed resource. </p>
   *
   * @param resource  The resource to transform.
   * @param outWidth  The width of the view or target the resource will be displayed in, or {@link
   *                  com.bumptech.glide.request.target.Target#SIZE_ORIGINAL} to indicate the
   *                  original resource width.
   * @param outHeight The height of the view or target the resource will be displayed in, or {@link
   *                  com.bumptech.glide.request.target.Target#SIZE_ORIGINAL} to indicate the
   *                  original resource height.
   * @return The transformed resource.
   */
  Resource<T> transform(Resource<T> resource, int outWidth, int outHeight);
}
