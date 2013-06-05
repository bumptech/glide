package com.bumptech.photos.resize.cache;

import android.graphics.Bitmap;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 6/5/13
 * Time: 11:29 AM
 * To change this template use File | Settings | File Templates.
 */
public interface MemoryCache {
    public interface ImageRemovedListener {
        public void onImageRemoved(Bitmap removed);
    }

    public boolean contains(Integer key);
    public Bitmap get(Integer key);

    /**
     * Add bitmap to the cache with the given key
     * @param key The key to retrieve the bitmap
     * @param bitmap The bitmap to store
     * @return The old value of key (null if key is not in map)
     */
    public Bitmap put(Integer key, Bitmap bitmap);
    public void setImageRemovedListener(ImageRemovedListener listener);
}
