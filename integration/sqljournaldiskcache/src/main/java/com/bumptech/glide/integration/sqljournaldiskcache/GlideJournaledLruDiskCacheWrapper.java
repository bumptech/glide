package com.bumptech.glide.integration.sqljournaldiskcache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.SafeKeyGenerator;
import com.bumptech.glide.util.Util;
import java.io.File;

/** Implements {@link DiskCache} using {@link JournaledLruDiskCache}. */
public final class GlideJournaledLruDiskCacheWrapper implements DiskCache {
  // 500 mb
  private static final long DEFAULT_GLIDE_CACHE_SIZE_BYTES = 1024 * 1024 * 500;
  public static final String DEFAULT_CACHE_DIR = "glide_cache";

  public static final String KEY_VALUE_STORE_PREFIX =
      "com.google.android.apps.photos.diskcache.GlideJournaledLruDiskCacheWrapper";

  private final JournaledLruDiskCache diskCache;
  private final SafeKeyGenerator safeKeyGenerator;
  private final DiskCacheDbHelper diskCacheDbHelper;

  public static GlideJournaledLruDiskCacheWrapper newInstance(Context context, File diskCacheDir) {
    return newInstance(
        context,
        diskCacheDir,
        // Default to not evicting based on entry age.
        /* staleEvictionThresholdMs= */ Long.MAX_VALUE,
        new DefaultClock());
  }

  public static GlideJournaledLruDiskCacheWrapper newInstance(
      Context context, File diskCacheDir, long staleEvictionThresholdMs, Clock clock) {
    return new GlideJournaledLruDiskCacheWrapper(
        diskCacheDir, DiskCacheDbHelper.forProd(context), staleEvictionThresholdMs, clock);
  }

  private GlideJournaledLruDiskCacheWrapper(
      File diskCacheDir,
      DiskCacheDbHelper diskCacheDbHelper,
      long staleEvictionThresholdMs,
      Clock clock) {
    this.diskCacheDbHelper = diskCacheDbHelper;
    this.safeKeyGenerator = new SafeKeyGenerator();
    this.diskCache =
        new JournaledLruDiskCache(
            diskCacheDir,
            diskCacheDbHelper,
            DEFAULT_GLIDE_CACHE_SIZE_BYTES,
            staleEvictionThresholdMs,
            clock);
  }

  /**
   * Sets the maximum size of the cache to a new size in bytes.
   *
   * <p>Must be called on a background thread.
   *
   * <p>The JournaledLruDiskCache manages the sizing of the cache. Decreasing the size may schedule
   * an eviction if the current cache size exceeds newMaximumSizeBytes. Evictions will be scheduled
   * and executed asynchronously. Therefore, the eviction will happen based on the latest maximum
   * cache size, not the maximum size at scheduling.
   */
  public void setMaximumSizeBytes(long newMaximumSizeBytes) {
    Util.assertBackgroundThread();
    diskCache.setMaximumSizeBytes(newMaximumSizeBytes);
  }

  @Override
  public File get(Key key) {
    String safeKey = safeKeyGenerator.getSafeKey(key);
    return diskCache.get(safeKey);
  }

  @Override
  public void put(Key key, Writer writer) {
    String safeKey = safeKeyGenerator.getSafeKey(key);
    File tempFile = diskCache.beginPut(safeKey);
    // Edit already in progress, or file is already written.
    try {
      if (tempFile != null && writer.write(tempFile)) {
        diskCache.commitPut(safeKey, tempFile);
      }
    } finally {
      diskCache.abortPutIfNotCommitted(safeKey, tempFile);
    }
  }

  @Override
  public void delete(Key key) {
    String safeKey = safeKeyGenerator.getSafeKey(key);
    diskCache.delete(safeKey);
  }

  @Override
  public void clear() {
    diskCache.clear();
  }

  /**
   * @deprecated this method will be replaced by a more specific version
   */
  @Deprecated
  public SQLiteDatabase getWritableDatabase() {
    return diskCacheDbHelper.getWritableDatabase();
  }

  /** Returns number of bytes used by the JournaledLruDiskCache currently. */
  public long getCurrentSizeBytes() {
    return diskCache.getCurrentSizeBytes();
  }
}
