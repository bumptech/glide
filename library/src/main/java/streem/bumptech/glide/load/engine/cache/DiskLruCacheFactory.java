package com.bumptech.glide.load.engine.cache;

import java.io.File;

/**
 * Creates an {@link com.bumptech.glide.disklrucache.DiskLruCache} based disk cache in the specified
 * disk cache directory.
 *
 * <p>If you need to make I/O access before returning the cache directory use the {@link
 * DiskLruCacheFactory#DiskLruCacheFactory(CacheDirectoryGetter, long)} constructor variant.
 */
// Public API.
@SuppressWarnings("unused")
public class DiskLruCacheFactory implements DiskCache.Factory {
  private final long diskCacheSize;
  private final CacheDirectoryGetter cacheDirectoryGetter;

  /** Interface called out of UI thread to get the cache folder. */
  public interface CacheDirectoryGetter {
    File getCacheDirectory();
  }

  public DiskLruCacheFactory(final String diskCacheFolder, long diskCacheSize) {
    this(
        new CacheDirectoryGetter() {
          @Override
          public File getCacheDirectory() {
            return new File(diskCacheFolder);
          }
        },
        diskCacheSize);
  }

  public DiskLruCacheFactory(
      final String diskCacheFolder, final String diskCacheName, long diskCacheSize) {
    this(
        new CacheDirectoryGetter() {
          @Override
          public File getCacheDirectory() {
            return new File(diskCacheFolder, diskCacheName);
          }
        },
        diskCacheSize);
  }

  /**
   * When using this constructor {@link CacheDirectoryGetter#getCacheDirectory()} will be called out
   * of UI thread, allowing to do I/O access without performance impacts.
   *
   * @param cacheDirectoryGetter Interface called out of UI thread to get the cache folder.
   * @param diskCacheSize Desired max bytes size for the LRU disk cache.
   */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  public DiskLruCacheFactory(CacheDirectoryGetter cacheDirectoryGetter, long diskCacheSize) {
    this.diskCacheSize = diskCacheSize;
    this.cacheDirectoryGetter = cacheDirectoryGetter;
  }

  @Override
  public DiskCache build() {
    File cacheDir = cacheDirectoryGetter.getCacheDirectory();

    if (cacheDir == null) {
      return null;
    }

    if (cacheDir.isDirectory() || cacheDir.mkdirs()) {
      return DiskLruCacheWrapper.create(cacheDir, diskCacheSize);
    }

    return null;
  }
}
