/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.resize.cache;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/**
 * A thin wrapper around the LruCache provided in the Android support libraries.
 *
 * @see android.support.v4.util.LruCache
 */
public class LruPhotoCache implements MemoryCache {
    private final LruCache<Integer, Bitmap> lruCache;
    private ImageRemovedListener imageRemovedListener;

    public LruPhotoCache(int maxSize) {
        lruCache = new LruCache<Integer, Bitmap>(maxSize) {
            @Override
            protected void entryRemoved(boolean evicted, Integer key, Bitmap oldValue, Bitmap newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
                if (imageRemovedListener != null) {
                    imageRemovedListener.onImageRemoved(oldValue);
                }
            }

            @Override
            protected int sizeOf(Integer key, Bitmap value) {
                //get the size, getByteCount() is API 12+...
                return value.getHeight() * value.getRowBytes();
            }
        };
    }

    public boolean contains(Integer key) {
        return get(key) != null;
    }

    @Override
    public Bitmap get(Integer key) {
        return lruCache.get(key);
    }

    @Override
    public Bitmap put(Integer key, Bitmap bitmap) {
        return lruCache.put(key, bitmap);
    }

    @Override
    public void setImageRemovedListener(ImageRemovedListener listener) {
        this.imageRemovedListener = listener;
    }
}
