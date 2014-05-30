package com.bumptech.glide.load.engine.cache;

import android.content.ComponentCallbacks2;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.Resource;

/**
 * An interface for adding and removing resources from an in memory cache
 */
public interface MemoryCache {
    /**
     * An interface that will be called whenever a bitmap is removed from the cache.
     */
    public interface ResourceRemovedListener {
        public void onResourceRemoved(Resource removed);
    }

    /**
     * Adjust the maximum size of the cache by multiplying the original size of the cache by the given multiplier.
     *
     * <p>
     *     If the size multiplier causes the size of the cache to be decreased, items will be evicted until the cache
     *     is smaller than the new size.
     * </p>
     *
     * @param multiplier A size multiplier >= 0.
     */
    public void setSizeMultiplier(float multiplier);

    /**
     * Tell if cache contains key
     * @param key The key
     * @return true iff the key has a non null value in the cache
     */
    public boolean contains(Key key);

    /**
     * Get a value from the cache
     * @param key The key
     * @return The bitmap at key or null if the key is not present
     */
    public Resource get(Key key);

    /**
     * Add bitmap to the cache with the given key
     * @param key The key to retrieve the bitmap
     * @param resource The {@link Resource} to store
     * @return The old value of key (null if key is not in map)
     */
    public Resource put(Key key, Resource resource);

    /**
     * Set the listener to be called when a bitmap is removed from the cache
     * @param listener The listener
     */
    public void setResourceRemovedListener(ResourceRemovedListener listener);

    /**
     * Evict all items from the memory cache.
     */
    public void clearMemory();

    /**
     * Trim the memory cache to the appropriate level. Typically called on the callback onTrimMemory.
     * @param level This integer represents a trim level as specified in {@link ComponentCallbacks2}
     */
    public void trimMemory(int level);
}
