package com.bumptech.glide.integration.sqljournaldiskcache;

import androidx.collection.ArrayMap;
import com.bumptech.glide.util.LruCache;
import java.io.File;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Maintains an LRU cache of {@link String} keys to {@link Entry Entrys} where each entry contains a
 * read/write lock used to guarantee state and entries that are currently locked are guaranteed not
 * to be evicted.
 */
final class EntryCache {
  private final ArrayMap<String, Entry> activeEntries = new ArrayMap<>();
  private final LruCache<String, Entry> inactiveEntries = new LruCache<>(6000);

  synchronized void clear() {
    activeEntries.clear();
    inactiveEntries.clearMemory();
  }

  synchronized Entry get(String key) {
    Entry entry = activeEntries.get(key);
    if (entry == null) {
      entry = inactiveEntries.get(key);
      if (entry == null) {
        entry = new Entry(key, this);
        activeEntries.put(key, entry);
      }
    }
    return entry;
  }

  private synchronized void removeFromActive(Entry entry) {
    activeEntries.remove(entry.key);
    inactiveEntries.put(entry.key, entry);
  }

  private synchronized void addToActive(Entry entry) {
    inactiveEntries.remove(entry.key);
    activeEntries.put(entry.key, entry);
  }

  static final class Entry {
    private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final String key;
    private final EntryCache cache;

    private int lockCount;
    private State state = State.UNKNOWN;
    private File file;

    Entry(String key, EntryCache cache) {
      this.key = key;
      this.cache = cache;
    }

    File getFile() {
      return file;
    }

    boolean isStateKnown() {
      return state != State.UNKNOWN;
    }

    boolean isPresent() {
      return state == State.PRESENT;
    }

    void setPresent(File file) {
      this.file = file;
      state = State.PRESENT;
    }

    void setUnknown() {
      state = State.UNKNOWN;
    }

    void setNotPresent() {
      state = State.NOT_PRESENT;
    }

    void acquireReadLock() {
      maybeSetActive();
      readWriteLock.readLock().lock();
    }

    void releaseReadLock() {
      ReentrantReadWriteLock lock = readWriteLock;
      maybeSetInactive();
      lock.readLock().unlock();
    }

    void acquireWriteLock() {
      maybeSetActive();
      readWriteLock.writeLock().lock();
    }

    void releaseWriteLock() {
      ReentrantReadWriteLock lock = readWriteLock;
      maybeSetInactive();
      lock.writeLock().unlock();
    }

    private synchronized void maybeSetActive() {
      lockCount++;
      if (lockCount == 1) {
        cache.addToActive(this);
        readWriteLock = new ReentrantReadWriteLock();
      }
    }

    private synchronized void maybeSetInactive() {
      lockCount--;
      if (lockCount == 0) {
        cache.removeFromActive(this);
        readWriteLock = null;
      }
    }

    private enum State {
      UNKNOWN,
      PRESENT,
      NOT_PRESENT,
    }
  }
}
