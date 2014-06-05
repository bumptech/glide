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

    public LruCache(int size) {
        this.initialMaxSize = size;
        this.maxSize = size;
    }

    public void setSizeMultiplier(float multiplier) {
        if (multiplier < 0) {
            throw new IllegalArgumentException("Multiplier must be >= 0");
        }
        maxSize = Math.round(initialMaxSize * multiplier);
        evict();
    }

    protected int getSize(Y item) {
        return 1;
    }

    protected void onItemRemoved(Y item) {  }

    public int getCurrentSize() {
        return currentSize;
    }

    public boolean contains(T key) {
        return cache.containsKey(key);
    }

    public Y get(T key) {
        return cache.get(key);
    }

    public Y put(T key, Y item) {
        final int itemSize = getSize(item);
        if (itemSize >= maxSize) {
            onItemRemoved(item);
            return null;
        }

        currentSize += getSize(item);
        final Y result = cache.put(key, item);
        evict();
        return result;
    }

    public void clearMemory() {
        trimToSize(0);
    }

    protected void trimToSize(int size) {
        Map.Entry<T, Y> last;
        while (currentSize > size) {
            last = cache.entrySet().iterator().next();
            final Y toRemove = last.getValue();
            currentSize -= getSize(toRemove);
            cache.remove(last.getKey());
            onItemRemoved(toRemove);
        }
    }

    private void evict() {
        trimToSize(maxSize);
    }
}
