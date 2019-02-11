package com.bumptech.glide.load.engine.cache;

import android.content.Context;

/**
 * Creates an {@link com.bumptech.glide.disklrucache.DiskLruCache} based disk cache in the external
 * disk cache directory, which falls back to the internal disk cache if no external storage is
 * available. If ever fell back to the internal disk cache, will use that one from that moment on.
 *
 * <p><b>Images can be read by everyone when using external disk cache.</b>
 */
// Public API.
@SuppressWarnings({"unused", "WeakerAccess"})
public final class ExternalExpirableCacheDiskCacheFactory extends ExpirableDiskLruCacheFactory {

  public ExternalExpirableCacheDiskCacheFactory(Context context, long expirationMillis) {
    this(context, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR,
        DiskCache.Factory.DEFAULT_DISK_CACHE_SIZE, expirationMillis);
  }

  public ExternalExpirableCacheDiskCacheFactory(Context context, long diskCacheSize,
                                                long expirationMillis) {
    this(context, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR, diskCacheSize, expirationMillis);
  }

  public ExternalExpirableCacheDiskCacheFactory(final Context context, final String diskCacheName,
                                                final long diskCacheSize, long expirationMillis) {
    super(new ExternalCacheDirectoryGetter(context, diskCacheName),
        diskCacheSize, expirationMillis);
  }
}
