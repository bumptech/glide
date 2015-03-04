package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;

import java.io.UnsupportedEncodingException;
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
  private final String transformationId;

  public ResourceCacheKey(Key sourceKey, Key signature, int width, int height,
      Transformation<?> appliedTransformation, Class<?> decodedResourceClass) {
    this.sourceKey = sourceKey;
    this.signature = signature;
    this.width = width;
    this.height = height;
    transformationId = appliedTransformation != null ? appliedTransformation.getId() : null;
    this.decodedResourceClass = decodedResourceClass;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ResourceCacheKey) {
      ResourceCacheKey other = (ResourceCacheKey) o;
      return height == other.height && width == other.width
          && (transformationId == null
              ? other.transformationId == null : transformationId.equals(other.transformationId))
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
    if (transformationId != null) {
      result = 31 * result + transformationId.hashCode();
    }
    result = 31 * result + decodedResourceClass.hashCode();
    return result;
  }

  // TODO: Include relevant options?
  @Override
  public void updateDiskCacheKey(MessageDigest messageDigest)
      throws UnsupportedEncodingException {
    byte[] dimensions = ByteBuffer.allocate(8).putInt(width).putInt(height).array();
    signature.updateDiskCacheKey(messageDigest);
    sourceKey.updateDiskCacheKey(messageDigest);
    messageDigest.update(dimensions);
    if (transformationId != null) {
      messageDigest.update(transformationId.getBytes(STRING_CHARSET_NAME));
    }
    messageDigest.update(decodedResourceClass.getName().getBytes(STRING_CHARSET_NAME));
  }

  @Override
  public String toString() {
    return "ResourceCacheKey{"
        + "sourceKey=" + sourceKey
        + ", signature=" + signature
        + ", width=" + width
        + ", height=" + height
        + ", decodedResourceClass=" + decodedResourceClass
        + ", transformationId='" + transformationId + '\''
        + '}';
  }
}
