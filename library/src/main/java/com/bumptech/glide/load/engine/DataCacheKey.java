package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

/**
 * A cache key for original source data + any requested signature.
 */
final class DataCacheKey implements Key {

  private final String id;
  private final Key signature;

  public DataCacheKey(String id, Key signature) {
    this.id = id;
    this.signature = signature;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DataCacheKey that = (DataCacheKey) o;

    if (!id.equals(that.id)) {
      return false;
    }
    if (!signature.equals(that.signature)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + signature.hashCode();
    return result;
  }

  @Override
  public void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException {
    messageDigest.update(id.getBytes(STRING_CHARSET_NAME));
    signature.updateDiskCacheKey(messageDigest);
  }
}
