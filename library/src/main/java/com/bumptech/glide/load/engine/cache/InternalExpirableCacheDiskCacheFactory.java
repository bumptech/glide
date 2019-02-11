package com.bumptech.glide.load.engine.cache;

import android.content.Context;

/**
 * Creates an {@link com.bumptech.glide.disklrucache.DiskLruCache} based disk cache in the internal
 * disk cache directory.
 */
// Public API.
@SuppressWarnings({"WeakerAccess", "unused"})
public class InternalExpirableCacheDiskCacheFactory extends ExpirableDiskLruCacheFactory {

  public InternalExpirableCacheDiskCacheFactory(Context context, long expirationMillis) {
    this(context, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR,
        DiskCache.Factory.DEFAULT_DISK_CACHE_SIZE, expirationMillis);
  }

  public InternalExpirableCacheDiskCacheFactory(Context context, long diskCacheSize,
                                                long expirationMillis) {
    this(context, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR, diskCacheSize, expirationMillis);
  }

  public InternalExpirableCacheDiskCacheFactory(final Context context, final String diskCacheName,
                                                long diskCacheSize, long expirationMillis) {
    super(new InternalCacheDirectoryGetter(context, diskCacheName),
        diskCacheSize, expirationMillis);
  }
}
