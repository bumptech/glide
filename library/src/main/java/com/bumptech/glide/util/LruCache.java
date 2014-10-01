package com.bumptech.glide.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A general purpose size limited cache that evicts items using an LRU algorithm. By default every item is assumed to
 * have a size of one. Subclasses can override {@link #getSize(Object)}} to change the size on a per item basis.
 *
 * @param <T> The type of the keys.
 * @param <Y> The type of the values.
 */
public class LruCache<T, Y> {
    private final LinkedHashMap<T, Y> cache = new LinkedHashMap<T, Y>(100, 0.75f, true);
    private int maxSize;
    private final int initialMaxSize;
    private int currentSize = 0;

    /**
     * Constructor for LruCache.
     *
     * @param size The maximum size of the cache, the units must match the units used in {@link #getSize(Object)}.
     */
    public LruCache(int size) {
        this.initialMaxSize = size;
        this.maxSize = size;
    }

    /**
     * Sets a size multiplier that will be applied to the size provided in the constructor to set the new size of the
     * cache. If the new size is less than the current size, entries will be evicted until the current size is less
     * than or equal to the new size.
     *
     * @param multiplier The multiplier to apply.
     */
    public void setSizeMultiplier(float multiplier) {
        if (multiplier < 0) {
            throw new IllegalArgumentException("Multiplier must be >= 0");
        }
        maxSize = Math.round(initialMaxSize * multiplier);
        evict();
    }

    /**
     * Returns the size of a given item, defaulting to one. The units must match those used in the size passed in to the
     * constructor. Subclasses can override this method to return sizes in various units, usually bytes.
     *
     * @param item The item to get the size of.
     */
    protected int getSize(Y item) {
        return 1;
    }

    /**
     * A callback called whenever an item is evicted from the cache. Subclasses can override.
     *
     * @param key The key of the evicted item.
     * @param item The evicted item.
     */
    protected void onItemEvicted(T key, Y item) {
        // optional override
    }

    /**
     * Returns the current maximum size of the cache in bytes.
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Returns the sum of the sizes of all items in the cache.
     */
    public int getCurrentSize() {
        return currentSize;
    }

    /**
     * Returns true if there is a value for the given key in the cache.
     *
     * @param key The key to check.
     */

    public boolean contains(T key) {
        return cache.containsKey(key);
    }

    /**
     * Returns the item in the cache for the given key or null if no such item exists.
     *
     * @param key The key to check.
     */
    public Y get(T key) {
        return cache.get(key);
    }

    /**
     * Adds the given item to the cache with the given key and returns any previous entry for the given key that may
     * have already been in the cache.
     *
     * <p>
     *     If the size of the item is larger than the total cache size, the item will not be added to the cache and
     *     instead {@link #onItemEvicted(Object, Object)} will be called synchronously with the given key and item.
     * </p>
     *
     * @param key The key to add the item at.
     * @param item The item to add.
     */
    public Y put(T key, Y item) {
        final int itemSize = getSize(item);
        if (itemSize >= maxSize) {
            onItemEvicted(key, item);
            return null;
        }

        final Y result = cache.put(key, item);
        if (item != null) {
            currentSize += getSize(item);
        }
        if (result != null) {
            // TODO: should we call onItemEvicted here?
            currentSize -= getSize(result);
        }
        evict();

        return result;
    }

    /**
     * Removes the item at the given key and returns the removed item if present, and null otherwise.
     *
     * @param key The key to remove the item at.
     */
    public Y remove(T key) {
        final Y value = cache.remove(key);
        if (value != null) {
            currentSize -= getSize(value);
        }
        return value;
    }

    /**
     * Clears all items in the cache.
     */
    public void clearMemory() {
        trimToSize(0);
    }

    /**
     * Removes the least recently used items from the cache until the current size is less than the given size.
     *
     * @param size The size the cache should be less than.
     */
    protected void trimToSize(int size) {
        Map.Entry<T, Y> last;
        while (currentSize > size) {
            last = cache.entrySet().iterator().next();
            final Y toRemove = last.getValue();
            currentSize -= getSize(toRemove);
            final T key = last.getKey();
            cache.remove(key);
            onItemEvicted(key, toRemove);
        }
    }

    private void evict() {
        trimToSize(maxSize);
    }
}
