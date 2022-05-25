package com.bumptech.glide.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A general purpose size limited cache that evicts items using an LRU algorithm. By default every
 * item is assumed to have a size of one. Subclasses can override {@link #getSize(Object)}} to
 * change the size on a per item basis.
 *
 * @param <T> The type of the keys.
 * @param <Y> The type of the values.
 */
public class LruCache<T, Y> {
  private final Map<T, Entry<Y>> cache = new LinkedHashMap<>(100, 0.75f, true);
  private final long initialMaxSize;
  private long maxSize;
  private long currentSize;

  /**
   * Constructor for LruCache.
   *
   * @param size The maximum size of the cache, the units must match the units used in {@link
   *     #getSize(Object)}.
   */
  public LruCache(long size) {
    this.initialMaxSize = size;
    this.maxSize = size;
  }

  /**
   * Sets a size multiplier that will be applied to the size provided in the constructor to put the
   * new size of the cache. If the new size is less than the current size, entries will be evicted
   * until the current size is less than or equal to the new size.
   *
   * @param multiplier The multiplier to apply.
   */
  public synchronized void setSizeMultiplier(float multiplier) {
    if (multiplier < 0) {
      throw new IllegalArgumentException("Multiplier must be >= 0");
    }
    maxSize = Math.round(initialMaxSize * multiplier);
    evict();
  }

  /**
   * Returns the size of a given item, defaulting to one. The units must match those used in the
   * size passed in to the constructor. Subclasses can override this method to return sizes in
   * various units, usually bytes.
   *
   * @param item The item to get the size of.
   */
  protected int getSize(@Nullable Y item) {
    return 1;
  }

  /** Returns the number of entries stored in cache. */
  protected synchronized int getCount() {
    return cache.size();
  }

  /**
   * A callback called whenever an item is evicted from the cache. Subclasses can override.
   *
   * @param key The key of the evicted item.
   * @param item The evicted item.
   */
  protected void onItemEvicted(@NonNull T key, @Nullable Y item) {
    // optional override
  }

  /** Returns the current maximum size of the cache in bytes. */
  public synchronized long getMaxSize() {
    return maxSize;
  }

  /** Returns the sum of the sizes of all items in the cache. */
  public synchronized long getCurrentSize() {
    return currentSize;
  }

  /**
   * Returns true if there is a value for the given key in the cache.
   *
   * @param key The key to check.
   */
  public synchronized boolean contains(@NonNull T key) {
    return cache.containsKey(key);
  }

  /**
   * Returns the item in the cache for the given key or null if no such item exists.
   *
   * @param key The key to check.
   */
  @Nullable
  public synchronized Y get(@NonNull T key) {
    Entry<Y> entry = cache.get(key);
    return entry != null ? entry.value : null;
  }

  /**
   * Adds the given item to the cache with the given key and returns any previous entry for the
   * given key that may have already been in the cache.
   *
   * <p>If the size of the item is larger than the total cache size, the item will not be added to
   * the cache and instead {@link #onItemEvicted(Object, Object)} will be called synchronously with
   * the given key and item.
   *
   * <p>The size of the item is determined by the {@link #getSize(Object)} method. To avoid errors
   * where {@link #getSize(Object)} returns different values for the same object when called at
   * different times, the size value is acquired in {@code put} and retained until the item is
   * evicted, replaced or removed.
   *
   * <p>If {@code item} is null the behavior here is a little odd. For the most part it's similar to
   * simply calling {@link #remove(Object)} with the given key. The difference is that calling this
   * method with a null {@code item} will result in an entry remaining in the cache with a null
   * value and 0 size. The only real consequence is that at some point {@link #onItemEvicted(Object,
   * Object)} may be called with the given {@code key} and a null value. Ideally we'd make calling
   * this method with a null {@code item} identical to {@link #remove(Object)} but we're preserving
   * this odd behavior to match older versions :(.
   *
   * @param key The key to add the item at.
   * @param item The item to add.
   */
  @Nullable
  public synchronized Y put(@NonNull T key, @Nullable Y item) {
    final int itemSize = getSize(item);
    if (itemSize >= maxSize) {
      onItemEvicted(key, item);
      return null;
    }

    if (item != null) {
      currentSize += itemSize;
    }
    @Nullable Entry<Y> old = cache.put(key, item == null ? null : new Entry<>(item, itemSize));
    if (old != null) {
      currentSize -= old.size;

      if (!old.value.equals(item)) {
        onItemEvicted(key, old.value);
      }
    }
    evict();

    return old != null ? old.value : null;
  }

  /**
   * Removes the item at the given key and returns the removed item if present, and null otherwise.
   *
   * @param key The key to remove the item at.
   */
  @Nullable
  public synchronized Y remove(@NonNull T key) {
    Entry<Y> entry = cache.remove(key);
    if (entry == null) {
      return null;
    }
    currentSize -= entry.size;
    return entry.value;
  }

  /** Clears all items in the cache. */
  public void clearMemory() {
    trimToSize(0);
  }

  /**
   * Removes the least recently used items from the cache until the current size is less than the
   * given size.
   *
   * @param size The size the cache should be less than.
   */
  protected synchronized void trimToSize(long size) {
    Map.Entry<T, Entry<Y>> last;
    Iterator<Map.Entry<T, Entry<Y>>> cacheIterator;
    while (currentSize > size) {
      cacheIterator = cache.entrySet().iterator();
      last = cacheIterator.next();
      final Entry<Y> toRemove = last.getValue();
      currentSize -= toRemove.size;
      final T key = last.getKey();
      cacheIterator.remove();
      onItemEvicted(key, toRemove.value);
    }
  }

  private void evict() {
    trimToSize(maxSize);
  }

  @Synthetic
  static final class Entry<Y> {
    final Y value;
    final int size;

    @Synthetic
    Entry(Y value, int size) {
      this.value = value;
      this.size = size;
    }
  }
}
