package com.bumptech.glide.load.engine.cache;

import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Keeps a map of keys to locks that allows locks to be removed from the map when no longer in use
 * so the size of the collection is bounded.
 *
 * <p>This class will be accessed by multiple threads in a thread pool and ensures that the number
 * of threads interested in each lock is updated atomically so that when the count reaches 0, the
 * lock can safely be removed from the map.
 */
final class DiskCacheWriteLocker {
  private final Map<String, WriteLock> locks = new HashMap<>();
  private final WriteLockPool writeLockPool = new WriteLockPool();

  void acquire(String safeKey) {
    WriteLock writeLock;
    synchronized (this) {
      writeLock = locks.get(safeKey);
      if (writeLock == null) {
        writeLock = writeLockPool.obtain();
        locks.put(safeKey, writeLock);
      }
      writeLock.interestedThreads++;
    }

    writeLock.lock.lock();
  }

  void release(String safeKey) {
    WriteLock writeLock;
    synchronized (this) {
      writeLock = Preconditions.checkNotNull(locks.get(safeKey));
      if (writeLock.interestedThreads < 1) {
        throw new IllegalStateException(
            "Cannot release a lock that is not held"
                + ", safeKey: "
                + safeKey
                + ", interestedThreads: "
                + writeLock.interestedThreads);
      }

      writeLock.interestedThreads--;
      if (writeLock.interestedThreads == 0) {
        WriteLock removed = locks.remove(safeKey);
        if (!removed.equals(writeLock)) {
          throw new IllegalStateException(
              "Removed the wrong lock"
                  + ", expected to remove: "
                  + writeLock
                  + ", but actually removed: "
                  + removed
                  + ", safeKey: "
                  + safeKey);
        }
        writeLockPool.offer(removed);
      }
    }

    writeLock.lock.unlock();
  }

  private static class WriteLock {
    final Lock lock = new ReentrantLock();
    int interestedThreads;

    @Synthetic
    WriteLock() {}
  }

  private static class WriteLockPool {
    private static final int MAX_POOL_SIZE = 10;
    private final Queue<WriteLock> pool = new ArrayDeque<>();

    @Synthetic
    WriteLockPool() {}

    WriteLock obtain() {
      WriteLock result;
      synchronized (pool) {
        result = pool.poll();
      }
      if (result == null) {
        result = new WriteLock();
      }
      return result;
    }

    void offer(WriteLock writeLock) {
      synchronized (pool) {
        if (pool.size() < MAX_POOL_SIZE) {
          pool.offer(writeLock);
        }
      }
    }
  }
}
