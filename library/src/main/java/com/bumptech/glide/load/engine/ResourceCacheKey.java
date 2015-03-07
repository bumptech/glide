package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

/**
 * A cache key for downsampled and transformed resource data + any requested signature.
 */
final class ResourceCacheKey implements Key {
  private final Key sourceKey;
  private final Key signature;
  private final int width;
  private final int height;
  private final Class<?> decodedResourceClass;
  private final Transformation<?> transformation;

  public ResourceCacheKey(Key sourceKey, Key signature, int width, int height,
      Transformation<?> appliedTransformation, Class<?> decodedResourceClass) {
    this.sourceKey = sourceKey;
    this.signature = signature;
    this.width = width;
    this.height = height;
    this.transformation = appliedTransformation;
    this.decodedResourceClass = decodedResourceClass;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ResourceCacheKey) {
      ResourceCacheKey other = (ResourceCacheKey) o;
      return height == other.height && width == other.width
          && (transformation == null
              ? other.transformation == null : transformation.equals(other.transformation))
          && decodedResourceClass.equals(other.decodedResourceClass)
          && sourceKey.equals(other.sourceKey) && signature.equals(other.signature);
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
    return result;
  }

  // TODO: Include relevant options?
  @Override
  public void updateDiskCacheKey(MessageDigest messageDigest) {
    byte[] dimensions = ByteBuffer.allocate(8).putInt(width).putInt(height).array();
    signature.updateDiskCacheKey(messageDigest);
    sourceKey.updateDiskCacheKey(messageDigest);
    messageDigest.update(dimensions);
    if (transformation != null) {
      transformation.updateDiskCacheKey(messageDigest);
    }
    messageDigest.update(decodedResourceClass.getName().getBytes(CHARSET));
  }

  @Override
  public String toString() {
    return "ResourceCacheKey{"
        + "sourceKey=" + sourceKey
        + ", signature=" + signature
        + ", width=" + width
        + ", height=" + height
        + ", decodedResourceClass=" + decodedResourceClass
        + ", transformation='" + transformation + '\''
        + '}';
  }
}
