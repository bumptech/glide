package com.bumptech.glide.load.engine.cache;

import android.support.annotation.Nullable;
import com.bumptech.glide.load.Key;
import java.io.File;

/**
 * The expirable DiskCache implementation. When entity is accessed, file last modified date is used
 * to check if it's not expired. If entity is expired it is removed and null is returned instead.
 * <p>
 * There must be no more than one active instance for a given directory at a time.
 *
 * @see #get(java.io.File, long)
 */
public class ExpirableDiskLruCacheWrapper extends DiskLruCacheWrapper {

  private final long expirationMillis;

  @SuppressWarnings({"WeakerAccess", "deprecation"})
  protected ExpirableDiskLruCacheWrapper(File directory, long maxSize, long expirationMillis) {
    super(directory, maxSize);
    this.expirationMillis = expirationMillis;
  }

  /**
   * Create a new DiskCache in the given directory with a specified max size and expiration
   * time.
   *
   * @param directory        The directory for the disk cache
   * @param maxSize          The max size for the disk cache
   * @param expirationMillis The expiration time in milliseconds
   * @return The new disk cache with the given arguments
   */
  public static DiskCache create(File directory, long maxSize, long expirationMillis) {
    return new ExpirableDiskLruCacheWrapper(directory, maxSize, expirationMillis);
  }

  @Override
  public File get(Key key) {
    File file = super.get(key);
    if (hasExpired(file)) {
      delete(key);
      file = null;
    }
    return file;
  }

  private boolean hasExpired(@Nullable File file) {
    return file != null && System.currentTimeMillis() > file.lastModified() + expirationMillis;
  }
}
