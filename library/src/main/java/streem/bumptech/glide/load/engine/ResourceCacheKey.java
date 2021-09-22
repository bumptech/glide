package com.bumptech.glide.load.engine;

import androidx.annotation.NonNull;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.util.LruCache;
import com.bumptech.glide.util.Util;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

/** A cache key for downsampled and transformed resource data + any requested signature. */
final class ResourceCacheKey implements Key {
  private static final LruCache<Class<?>, byte[]> RESOURCE_CLASS_BYTES = new LruCache<>(50);
  private final ArrayPool arrayPool;
  private final Key sourceKey;
  private final Key signature;
  private final int width;
  private final int height;
  private final Class<?> decodedResourceClass;
  private final Options options;
  private final Transformation<?> transformation;

  ResourceCacheKey(
      ArrayPool arrayPool,
      Key sourceKey,
      Key signature,
      int width,
      int height,
      Transformation<?> appliedTransformation,
      Class<?> decodedResourceClass,
      Options options) {
    this.arrayPool = arrayPool;
    this.sourceKey = sourceKey;
    this.signature = signature;
    this.width = width;
    this.height = height;
    this.transformation = appliedTransformation;
    this.decodedResourceClass = decodedResourceClass;
    this.options = options;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ResourceCacheKey) {
      ResourceCacheKey other = (ResourceCacheKey) o;
      return height == other.height
          && width == other.width
          && Util.bothNullOrEqual(transformation, other.transformation)
          && decodedResourceClass.equals(other.decodedResourceClass)
          && sourceKey.equals(other.sourceKey)
          && signature.equals(other.signature)
          && options.equals(other.options);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = sourceKey.hashCode();
    result = 31 * result + signature.hashCode();
    result = 31 * result + width;
    result = 31 * result + height;
    if (transformation != null) {
      result = 31 * result + transformation.hashCode();
    }
    result = 31 * result + decodedResourceClass.hashCode();
    result = 31 * result + options.hashCode();
    return result;
  }

  // TODO: Include relevant options?
  @Override
  public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    byte[] dimensions = arrayPool.getExact(8, byte[].class);
    ByteBuffer.wrap(dimensions).putInt(width).putInt(height).array();
    signature.updateDiskCacheKey(messageDigest);
    sourceKey.updateDiskCacheKey(messageDigest);
    messageDigest.update(dimensions);
    if (transformation != null) {
      transformation.updateDiskCacheKey(messageDigest);
    }
    options.updateDiskCacheKey(messageDigest);
    messageDigest.update(getResourceClassBytes());
    arrayPool.put(dimensions);
  }

  private byte[] getResourceClassBytes() {
    byte[] result = RESOURCE_CLASS_BYTES.get(decodedResourceClass);
    if (result == null) {
      result = decodedResourceClass.getName().getBytes(CHARSET);
      RESOURCE_CLASS_BYTES.put(decodedResourceClass, result);
    }
    return result;
  }

  @Override
  public String toString() {
    return "ResourceCacheKey{"
        + "sourceKey="
        + sourceKey
        + ", signature="
        + signature
        + ", width="
        + width
        + ", height="
        + height
        + ", decodedResourceClass="
        + decodedResourceClass
        + ", transformation='"
        + transformation
        + '\''
        + ", options="
        + options
        + '}';
  }
}
