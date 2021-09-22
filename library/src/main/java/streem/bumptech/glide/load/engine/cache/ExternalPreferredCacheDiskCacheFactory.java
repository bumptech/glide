package com.bumptech.glide.load.engine.cache;

import android.content.Context;
import androidx.annotation.Nullable;
import java.io.File;

/**
 * Creates an {@link com.bumptech.glide.disklrucache.DiskLruCache} based disk cache in the external
 * disk cache directory, which falls back to the internal disk cache if no external storage is
 * available. If ever fell back to the internal disk cache, will use that one from that moment on.
 *
 * <p><b>Images can be read by everyone when using external disk cache.</b>
 */
// Public API.
@SuppressWarnings({"unused", "WeakerAccess"})
public final class ExternalPreferredCacheDiskCacheFactory extends DiskLruCacheFactory {

  public ExternalPreferredCacheDiskCacheFactory(Context context) {
    this(
        context,
        DiskCache.Factory.DEFAULT_DISK_CACHE_DIR,
        DiskCache.Factory.DEFAULT_DISK_CACHE_SIZE);
  }

  public ExternalPreferredCacheDiskCacheFactory(Context context, long diskCacheSize) {
    this(context, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR, diskCacheSize);
  }

  public ExternalPreferredCacheDiskCacheFactory(
      final Context context, final String diskCacheName, final long diskCacheSize) {
    super(
        new CacheDirectoryGetter() {
          @Nullable
          private File getInternalCacheDirectory() {
            File cacheDirectory = context.getCacheDir();
            if (cacheDirectory == null) {
              return null;
            }
            if (diskCacheName != null) {
              return new File(cacheDirectory, diskCacheName);
            }
            return cacheDirectory;
          }

          @Override
          public File getCacheDirectory() {
            File internalCacheDirectory = getInternalCacheDirectory();

            // Already used internal cache, so keep using that one,
            // thus avoiding using both external and internal with transient errors.
            if ((null != internalCacheDirectory) && internalCacheDirectory.exists()) {
              return internalCacheDirectory;
            }

            File cacheDirectory = context.getExternalCacheDir();

            // Shared storage is not available.
            if ((cacheDirectory == null) || (!cacheDirectory.canWrite())) {
              return internalCacheDirectory;
            }
            if (diskCacheName != null) {
              return new File(cacheDirectory, diskCacheName);
            }
            return cacheDirectory;
          }
        },
        diskCacheSize);
  }
}
