package com.bumptech.glide.load.engine.cache;

import java.io.File;

/**
 * Creates an {@link com.bumptech.glide.disklrucache.DiskLruCache} based disk cache in the specified
 * disk cache directory.
 *
 * <p>If you need to make I/O access before returning the cache directory use the {@link
 * ExpirableDiskLruCacheFactory#ExpirableDiskLruCacheFactory(CacheDirectoryGetter, long, long)}
 * constructor variant.
 */
// Public API.
@SuppressWarnings("unused")
public class ExpirableDiskLruCacheFactory implements DiskCache.Factory {
  private final long diskCacheSize;
  private final long expirationMillis;
  private final CacheDirectoryGetter cacheDirectoryGetter;

  public ExpirableDiskLruCacheFactory(final String diskCacheFolder, long diskCacheSize,
                                      long expirationMillis) {
    this(new CacheDirectoryGetter() {
      @Override
      public File getCacheDirectory() {
        return new File(diskCacheFolder);
      }
    }, diskCacheSize, expirationMillis);
  }

  public ExpirableDiskLruCacheFactory(final String diskCacheFolder, final String diskCacheName,
                                      long diskCacheSize, long expirationMillis) {
    this(new CacheDirectoryGetter() {
      @Override
      public File getCacheDirectory() {
        return new File(diskCacheFolder, diskCacheName);
      }
    }, diskCacheSize, expirationMillis);
  }

  /**
   * When using this constructor {@link CacheDirectoryGetter#getCacheDirectory()} will be called out
   * of UI thread, allowing to do I/O access without performance impacts.
   *
   * @param cacheDirectoryGetter Interface called out of UI thread to get the cache folder.
   * @param diskCacheSize        Desired max bytes size for the LRU disk cache.
   * @param expirationMillis     The expiration time in milliseconds
   */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  public ExpirableDiskLruCacheFactory(CacheDirectoryGetter cacheDirectoryGetter,
                                      long diskCacheSize, long expirationMillis) {
    this.diskCacheSize = diskCacheSize;
    this.expirationMillis = expirationMillis;
    this.cacheDirectoryGetter = cacheDirectoryGetter;
  }

  @Override
  public DiskCache build() {
    File cacheDir = cacheDirectoryGetter.getCacheDirectory();

    if (cacheDir == null) {
      return null;
    }

    if (!cacheDir.mkdirs() && (!cacheDir.exists() || !cacheDir.isDirectory())) {
      return null;
    }

    return ExpirableDiskLruCacheWrapper.create(cacheDir, diskCacheSize, expirationMillis);
  }
}
