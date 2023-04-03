package com.bumptech.glide.integration.sqljournaldiskcache;

import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.util.Preconditions;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An Lru disk cache that stores each entry as a single File and uses a SQL based journal to track
 * sizes and eviction order.
 *
 * <p>Operations are not guaranteed and will silently fail in unexpected cases.
 *
 * <p>The size of the cache is approximate and will be exceeded for short periods of time. Failure
 * cases may leave behind temporary files that should be cleaned up in the future when the cache is
 * re-opened or when operations are attempted again.
 *
 * <p>This class is thread safe and may be accessed from multiple threads simultaneously.
 */
final class JournaledLruDiskCache {
  private static final String TAG = "DiskCache";
  private static final String CANARY_FILE_NAME = "cache_canary";
  // You must restart the app after enabling these logs for the change to take affect.
  // We cache isLoggable to avoid the performance hit of checking repeatedly.
  private static final boolean LOG_WARN = Log.isLoggable(TAG, Log.WARN);
  private static final boolean LOG_VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
  // The fraction of the maximum byte size of the cache we will allow the cache to go over before
  // triggering an eviction.
  private static final float DEFAULT_EVICTION_SLOP_MULTIPLIER = 0.05f;
  // The number of items we will queue to update the date modified time of in batches.
  private static final int DEFAULT_UPDATE_MODIFIED_TIME_BATCH_SIZE = 20;

  static final String TEMP_FILE_INDICATOR = ".tmp";

  private final File cacheDirectory;
  private final FileSystem fileSystem;
  private final Journal journal;
  // We use this File to determine if the system has wiped out our cache directory, which it may do
  // at any time. If the File is not present, then either we've never opened the cache for the given
  // directory before, or the cache was wiped.
  private final File canaryFile;
  private final EvictionManager evictionManager;
  private final RecoveryManager recoveryManager;
  private final EntryCache entries = new EntryCache();

  private volatile boolean isOpen;

  /**
   * @param cacheDirectory The directory in which the cache should store its files (Warning: the
   *     cache will delete all Files in the given directory. The directory should not be used to
   *     store any other content).
   * @param maximumSizeBytes The target maximum size in bytes. The cache size may briefly exceed
   *     this size by up to around 25mb depending on the size, thread scheduling, and the number of
   *     failed requests.
   */
  JournaledLruDiskCache(
      File cacheDirectory,
      DiskCacheDbHelper diskCacheDbHelper,
      long maximumSizeBytes,
      long staleEvictionThresholdMs,
      Clock clock) {
    this(
        cacheDirectory,
        diskCacheDbHelper,
        new FileSystem() {},
        maximumSizeBytes,
        getBackgroundLooper(),
        DEFAULT_EVICTION_SLOP_MULTIPLIER,
        DEFAULT_UPDATE_MODIFIED_TIME_BATCH_SIZE,
        staleEvictionThresholdMs,
        clock);
  }

  @VisibleForTesting
  JournaledLruDiskCache(
      File cacheDirectory,
      DiskCacheDbHelper diskCacheDbHelper,
      FileSystem fileSystem,
      long maximumSizeBytes,
      Looper workLooper,
      float slopMultiplier,
      int updateModifiedTimeBatchSize,
      long staleEvictionThresholdMs,
      Clock clock) {
    Preconditions.checkArgument(
        updateModifiedTimeBatchSize >= 1, "updated modified time batch size must be >= 1");
    this.cacheDirectory = cacheDirectory;
    this.fileSystem = fileSystem;

    journal = new Journal(diskCacheDbHelper, workLooper, updateModifiedTimeBatchSize, clock);
    canaryFile = new File(cacheDirectory, CANARY_FILE_NAME);

    evictionManager =
        new EvictionManager(
            this,
            cacheDirectory,
            fileSystem,
            journal,
            workLooper,
            maximumSizeBytes,
            slopMultiplier,
            staleEvictionThresholdMs,
            clock);
    recoveryManager = new RecoveryManager(this, cacheDirectory, journal, workLooper);
  }

  private static Looper getBackgroundLooper() {
    HandlerThread workThread =
        new HandlerThread("disk_cache_journal", Process.THREAD_PRIORITY_BACKGROUND);
    workThread.start();
    return workThread.getLooper();
  }

  private void openIfNotOpen() {
    if (!isOpen) {
      synchronized (this) {
        if (!isOpen) {
          boolean createdDirectory =
              cacheDirectory.mkdirs() || (cacheDirectory.exists() && cacheDirectory.isDirectory());
          if (!createdDirectory) {
            throw new IllegalStateException("Failed to create cache directory: " + cacheDirectory);
          }
          journal.open();
          isOpen = true;
          recoveryManager.triggerRecovery();
        }
      }
    }
  }

  // TODO(judds): rather than polling, we should use Android's FileObserver.
  private void verifyCanaryOrClear() {
    if (fileSystem.exists(canaryFile)) {
      return;
    }

    synchronized (this) {
      if (fileSystem.exists(canaryFile)) {
        return;
      }
      if (LOG_WARN) {
        Log.w(TAG, "Failed to find canary file, clearing disk cache");
      }
      clear();
    }
  }

  private void touchCanaryFile() {
    try {
      if (!fileSystem.createNewFile(canaryFile) && LOG_WARN) {
        Log.w(TAG, "Failed to create new canary file");
      }
    } catch (IOException e) {
      if (LOG_WARN) {
        Log.w(TAG, "Threw creating canary", e);
      }
    }
  }

  long getCurrentSizeBytes() {
    return journal.getCurrentSizeBytes();
  }

  /**
   * Makes a best effort attempt to delete all Files and clear the journal.
   *
   * <p>In progress writes may still complete and/or leave behind partial data.
   */
  public synchronized void clear() {
    if (LOG_WARN) {
      Log.w(TAG, "Clearing cache and deleting all entries!");
    }
    fileSystem.deleteAll(cacheDirectory);
    journal.clear();
    isOpen = false;
    entries.clear();
    openIfNotOpen();
    touchCanaryFile();
  }

  /**
   * Attempts to delete any content currently in the cache for the given key.
   *
   * <p>If no entry for the given key is found, this method will silently fail. If an entry is
   * found, it is possible the File deletion will fail and be re-attempted in the future.
   */
  public void delete(String key) {
    delete(Collections.singletonList(key));
  }

  List<String> delete(List<String> keys) {
    journal.markPendingDelete(keys);
    List<String> successfullyDeleted = new ArrayList<>(keys.size());
    for (String key : keys) {
      EntryCache.Entry entry = entries.get(key);
      entry.acquireWriteLock();
      try {
        File file = getCacheFile(key);
        if (fileSystem.delete(file)) {
          successfullyDeleted.add(key);
        } else if (LOG_WARN) {
          Log.w(TAG, "Failed to delete file: " + file);
        }
        entry.setNotPresent();
      } finally {
        entry.releaseWriteLock();
      }
    }
    journal.delete(successfullyDeleted);
    return successfullyDeleted;
  }

  /**
   * Returns a File committed previously for the given key, or {@code null} if no such File exists.
   *
   * <p>If a write is in progress but not yet committed for the given key, this method will return
   * {@code null} immediately, just as if the key were simply not present.
   */
  public File get(String key) {
    long startTime = getLogTime();
    openIfNotOpen();
    final File result;
    EntryCache.Entry entry = entries.get(key);
    entry.acquireReadLock();
    try {
      if (entry.isStateKnown()) {
        result = entry.isPresent() ? entry.getFile() : null;
      } else {
        File cacheFile = getCacheFile(key);
        if (fileSystem.exists(cacheFile)) {
          entry.setPresent(cacheFile);
          result = cacheFile;
        } else {
          entry.setNotPresent();
          result = null;
        }
      }
      if (result != null) {
        journal.get(key);
      }

      if (LOG_VERBOSE) {
        Log.v(TAG, "Completed get in: " + getElapsedTime(startTime) + ", key: " + key);
      }
    } finally {
      entry.releaseReadLock();
    }

    return result;
  }

  /**
   * Starts a put for the given key and returns a temporary {@link File} to which the caller can
   * write data, or {@code null} if an edit is already in progress for the given Key, or if a
   * committed entry already exists for the given key.
   *
   * <p>Callers should call {@link #commitPut(String, File)} with the given key and the {@link File}
   * returned from this method after they finish writing data to make the data they have written
   * available to calls to {@link #get(String)}. If an error occurs while writing data, callers can
   * omit calling {@link #commitPut(String, File)} and use {@link #abortPutIfNotCommitted(String,
   * File)} to cleanup any partial {@link File Files}.
   *
   * <p>Callers must call {@link #abortPutIfNotCommitted(String, File)} regardless of whether or not
   * their write succeeds. The expected pattern is as follows:
   *
   * <pre>{@code
   * File tempFile = cache.beginPut(key);
   * try {
   *   if (tempFile != null && writeToFile(someData, tempFile)) {
   *    cache.commitPut(key, tempFile);
   *   }
   * } finally {
   *   cache.abortIfNotCommitted(key, tempFile);
   * }
   * }</pre>
   *
   * <p>Until the caller calls {@link #abortPutIfNotCommitted(String, File)}, a lock is held that
   * will block future calls to this method for the given key.
   *
   * <p>The returned {@link File} may contain partial data if a previous write to this key failed.
   * Callers should not assume it is safe to append to the File without first clearing it.
   */
  @Nullable
  public File beginPut(String key) {
    long startTime = getLogTime();
    openIfNotOpen();
    verifyCanaryOrClear();
    EntryCache.Entry entry = entries.get(key);
    entry.acquireWriteLock();

    File permanentFile = getCacheFile(key);
    if (fileSystem.exists(permanentFile)) {
      return null;
    }

    File result = getTempFile(key);
    if (LOG_VERBOSE) {
      Log.v(TAG, "Completed begin put in: " + getElapsedTime(startTime) + ", key: " + key);
    }
    return result;
  }

  /**
   * Updates the size of the cache based on the data in the given temporary file and renames the
   * given temporary File to its permanent equivalent and makes it available to calls from {@link
   * #get(String)}.
   *
   * <p>The given {@link File} must be a {@link File} returned from {@link #get(String)} for the
   * given key. No validation is performed to verify either that the given {@link File} is a
   * legitimate temporary file from this cache or that the given {@link File} matches the given key.
   *
   * <p>It is possible this commit may fail silently, there is no guarantee that the data in the
   * given {@link File} will actually be available from {@link #get(String)}} when this method
   * completes. In practice commits should fail rarely unless insufficient storage is available or
   * the cache's directory or files are manipulated by a third party.
   *
   * <p>If the commit does fail, it will do so in one of two ways:
   *
   * <ul>
   *   <li>Prior to or while writing the entry to the journal
   *   <li>After writing the entry to the journal prior to or while renaming the temporary file to
   *       the permanent file.
   * </ul>
   *
   * If the commit fails prior to writing the entry to the journal, the dangling temporary File will
   * be found during recovery and deleted. If the commit fails after writing the entry to the
   * journal, the temporary file will be found during recovery and deleted and the corresponding
   * journal entry will also be deleted. The absence of a temporary File for a given key is assumed
   * to mean that either no entry exists, or the entry is committed and may be read.
   *
   * @throws IllegalStateException If this method wasn't preceded by a call to {@link
   *     #beginPut(String)} for the given key.
   */
  public void commitPut(String key, File temp) {
    long startTime = getLogTime();

    long totalBytesAdded = fileSystem.length(temp);
    journal.put(key, totalBytesAdded);

    if (LOG_VERBOSE) {
      Log.v(TAG, "Completed insertIntoDb in: " + getElapsedTime(startTime));
    }

    long startRenameTime = getLogTime();
    File permanentFile = getCacheFile(key);

    boolean isRenameSuccessful = fileSystem.rename(temp, permanentFile);
    // If we fail to rename the file, we will try to recover in our next recovery phase.
    if (isRenameSuccessful) {
      if (LOG_VERBOSE) {
        Log.v(TAG, "Successfully renamed in: " + getElapsedTime(startRenameTime));
      }
      EntryCache.Entry entry = entries.get(key);
      entry.setPresent(permanentFile);
    } else if (LOG_WARN) {
      Log.w(TAG, "Failed to rename file" + ", from: " + temp + ", to: " + permanentFile);
    }

    evictionManager.maybeScheduleEviction();

    if (LOG_VERBOSE) {
      Log.v(
          TAG,
          "Completed commitPut in: "
              + getElapsedTime(startTime)
              + ", current size: "
              + journal.getCurrentSizeBytes()
              + ", key: "
              + key);
    }
  }

  /**
   * Releases the write lock for the given key and, if the write was not committed, cleans up the
   * given temporary File and the corresponding journal entry for the given Key.
   *
   * <p>A write is assumed to have not been committed if the given temporary File still exists.
   */
  public void abortPutIfNotCommitted(String key, File temp) {
    try {
      // If the temporary File still exists, we haven't committed. If it doesn't exist, we either
      // didn't start writing and have nothing to roll back, or we finished writing and finished
      // the rename so the edit is committed.
      if (temp != null && fileSystem.delete(temp)) {
        journal.abortPut(key);
        EntryCache.Entry entry = entries.get(key);
        entry.setUnknown();
      }
    } finally {
      EntryCache.Entry entry = entries.get(key);
      entry.releaseWriteLock();
    }
  }

  void recoverPartialWrite(File temp) {
    String key = keyFromFile(temp);
    EntryCache.Entry entry = entries.get(key);
    entry.acquireWriteLock();
    try {
      // Try to delete the temporary file, if it fails, we will try again in the next recovery
      // phase.
      boolean deleted = temp.delete();
      if (!deleted) {
        if (LOG_WARN) {
          Log.w(TAG, "Failed to cleanup in progress write: " + temp);
        }
        // The write lock prevents us from directly racing with an in progress write. However when
        // the write lock is released, we will get to run. If the write completed successfully,
        // the
        // temp file will no longer exist, but the entry will. We do not want to delete the entry
        // just because we happened to try to run recovery during the write.
        return;
      }
      delete(key);
    } finally {
      entry.releaseWriteLock();
    }
  }

  private String keyFromFile(File file) {
    String name = file.getName();
    final String key;
    if (name.endsWith(TEMP_FILE_INDICATOR)) {
      key = name.substring(0, name.length() - TEMP_FILE_INDICATOR.length());
    } else {
      key = name;
    }
    return key;
  }

  /**
   * Sets the maximum size of the cache to a new size in bytes.
   *
   * <p>Must be called on a background thread.
   *
   * <p>The EvictionManager manages the sizing of the cache. Decreasing the size may schedule an
   * eviction if the current cache size exceeds newMaximumSizeBytes. Evictions will be scheduled and
   * executed asynchronously. Therefore, the eviction will happen based on the latest maximum cache
   * size, not the maximum size at scheduling.
   */
  public void setMaximumSizeBytes(long newMaximumSizeBytes) {
    evictionManager.setMaximumSizeBytes(newMaximumSizeBytes);
  }

  private File getCacheFile(String key) {
    return new File(cacheDirectory, key);
  }

  private File getTempFile(String key) {
    return new File(cacheDirectory, key + TEMP_FILE_INDICATOR);
  }

  private static long getLogTime() {
    return SystemClock.currentThreadTimeMillis();
  }

  private static long getElapsedTime(long startTime) {
    return getLogTime() - startTime;
  }
}
