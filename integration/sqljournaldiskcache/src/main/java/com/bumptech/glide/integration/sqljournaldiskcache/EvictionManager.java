package com.bumptech.glide.integration.sqljournaldiskcache;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import androidx.annotation.GuardedBy;
import java.io.File;
import java.util.List;

final class EvictionManager {
  private static final String TAG = "Evictor";
  // You must restart the app after enabling these logs for the change to take affect.
  // We cache isLoggable to avoid the performance hit of checking repeatedly.
  private static final boolean LOG_DEBUG = Log.isLoggable(TAG, Log.DEBUG);
  private static final boolean LOG_VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

  // The maximum amount we can go over our cache size before triggering evictions, currently 25mb.
  private static final long MAXIMUM_EVICTION_SLOP = 25 * 1024 * 1024;

  private final Handler evictionHandler;
  private final JournaledLruDiskCache diskCache;
  private final File cacheDirectory;
  private final FileSystem fileSystem;
  private final Journal journal;
  private final Looper workLooper;
  private final Clock clock;
  private final long evictionSlopBytes;
  private final long staleEvictionThresholdMs;

  @GuardedBy("this")
  private long maximumSizeBytes;

  EvictionManager(
      JournaledLruDiskCache diskCache,
      File cacheDirectory,
      FileSystem fileSystem,
      Journal journal,
      Looper workLooper,
      long maximumSizeBytes,
      float slopMultiplier,
      long staleEvictionThresholdMs,
      Clock clock) {
    this.diskCache = diskCache;
    this.cacheDirectory = cacheDirectory;
    this.fileSystem = fileSystem;
    this.journal = journal;
    this.workLooper = workLooper;
    this.maximumSizeBytes = maximumSizeBytes;
    this.clock = clock;
    this.staleEvictionThresholdMs = staleEvictionThresholdMs;

    evictionSlopBytes =
        Math.min(Math.round(maximumSizeBytes * slopMultiplier), MAXIMUM_EVICTION_SLOP);
    evictionHandler = new Handler(workLooper, new EvictionCallback());
  }

  /**
   * Sets maximumSizeBytes to a new size.
   *
   * <p>Must be called on a background thread.
   *
   * <p>Decreasing the maximumSizeBytes may schedule an eviction if the current cache size exceeds
   * the new maximumSizeBytes. Evictions will be scheduled and executed asynchronously. Therefore,
   * the eviction will happen based on the latest maximum cache size, not the maximum size at
   * scheduling.
   */
  synchronized void setMaximumSizeBytes(long newMaxSizeBytes) {
    long originalMaxBytes = maximumSizeBytes;
    maximumSizeBytes = newMaxSizeBytes;
    if (newMaxSizeBytes < originalMaxBytes) {
      maybeScheduleEviction(newMaxSizeBytes);
    }
  }

  private synchronized long getMaximumSizeBytes() {
    return maximumSizeBytes;
  }

  /**
   * Schedules a journal eviction on a work thread if the journal size currently exceeds the allowed
   * cache size.
   */
  void maybeScheduleEviction() {
    maybeScheduleEviction(getMaximumSizeBytes());
  }

  private void maybeScheduleEviction(long maximumSizeBytes) {
    if (isEvictionRequired(maximumSizeBytes)) {
      evictionHandler.obtainMessage(MessageIds.EVICT).sendToTarget();
    }
  }

  private boolean isEvictionRequired(long maximumSizeBytes) {
    return journal.getCurrentSizeBytes() > evictionSlopBytes + maximumSizeBytes;
  }

  private void evictOnWorkThread() {
    if (!Looper.myLooper().equals(workLooper)) {
      throw new IllegalStateException(
          "Cannot call evictOnWorkThread on thread: " + Thread.currentThread().getName());
    }
    long maximumSizeBytes = getMaximumSizeBytes();
    long staleDateMs = clock.currentTimeMillis() - staleEvictionThresholdMs;
    List<String> staleEntriesKeys = journal.getStaleEntries(staleDateMs);
    // Writes may queue up a number of eviction messages. After the first one runs, eviction may no
    // longer be necessary, so we simply ignore the message.
    if (!isEvictionRequired(maximumSizeBytes) && staleEntriesKeys.isEmpty()) {
      if (LOG_VERBOSE) {
        Log.v(TAG, "Ignoring eviction, not needed");
      }
      return;
    }
    if (LOG_DEBUG) {
      Log.d(TAG, "Starting eviction on work thread");
    }

    int successfullyDeletedCount = 0;
    int triedToDeleteEntries = staleEntriesKeys.size();
    if (!staleEntriesKeys.isEmpty()) {
      successfullyDeletedCount += diskCache.delete(staleEntriesKeys).size();
    }

    long targetSize = maximumSizeBytes - evictionSlopBytes;
    if (isEvictionRequired(maximumSizeBytes)) {
      long bytesToEvict = journal.getCurrentSizeBytes() - targetSize;
      List<String> leastRecentlyUsedKeys = journal.getLeastRecentlyUsed(bytesToEvict);
      triedToDeleteEntries += leastRecentlyUsedKeys.size();
      successfullyDeletedCount += diskCache.delete(leastRecentlyUsedKeys).size();
    }

    if (triedToDeleteEntries == 0) {
      throw new IllegalStateException("Failed to find entries to evict.");
    }

    if (LOG_DEBUG) {
      Log.d(
          TAG,
          "Ran eviction"
              + ", tried to delete: "
              + triedToDeleteEntries
              + " entries"
              + ", actually deleted: "
              + successfullyDeletedCount
              + " entries"
              + ", target journal : "
              + targetSize
              + ", journal size: "
              + journal.getCurrentSizeBytes()
              + ", file size: "
              + fileSystem.getDirectorySize(cacheDirectory));
    }
  }

  private class EvictionCallback implements Handler.Callback {
    @Override
    public boolean handleMessage(Message msg) {
      if (msg.what != MessageIds.EVICT) {
        return false;
      }
      evictOnWorkThread();
      return true;
    }
  }
}
