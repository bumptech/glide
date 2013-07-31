package com.bumptech.glide.resize.cache;

import android.graphics.Bitmap;

/**
 * An interface for adding and removing from an in memory cache
 */
public interface MemoryCache {
    /**
     * An interface that will be called whenever a bitmap is removed from the cache.
     */
    public interface ImageRemovedListener {
        public void onImageRemoved(Bitmap removed);
    }

    /**
     * Tell if cache contains key
     * @param key The key
     * @return true iff the key has a non null value in the cache
     */
    public boolean contains(String key);

    /**
     * Get a value from the cache
     * @param key The key
     * @return The bitmap at key or null if the key is not present
     */
    public Bitmap get(String key);

    /**
     * Add bitmap to the cache with the given key
     * @param key The key to retrieve the bitmap
     * @param bitmap The bitmap to store
     * @return The old value of key (null if key is not in map)
     */
    public Bitmap put(String key, Bitmap bitmap);

    /**
     * Set the listener to be called when a bitmap is removed from the cache
     * @param listener The listener
     */
    public void setImageRemovedListener(ImageRemovedListener listener);
}
