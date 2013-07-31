/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.resize.cache;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/**
 * A thin wrapper around the LruCache provided in the Android support libraries.
 *
 * @see android.support.v4.util.LruCache
 */
public class LruPhotoCache implements MemoryCache {
    private final LruCache<String, Bitmap> lruCache;
    private ImageRemovedListener imageRemovedListener;

    public LruPhotoCache(int maxSize) {
        lruCache = new LruCache<String, Bitmap>(maxSize) {
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
                if (imageRemovedListener != null) {
                    imageRemovedListener.onImageRemoved(oldValue);
                }
            }

            @Override
            protected int sizeOf(String key, Bitmap value) {
                //get the size, getByteCount() is API 12+...
                return value.getHeight() * value.getRowBytes();
            }
        };
    }

    public boolean contains(String key) {
        return lruCache.get(key) != null;
    }

    @Override
    public Bitmap get(String key) {
        return lruCache.get(key);
    }

    @Override
    public Bitmap put(String key, Bitmap bitmap) {
        return lruCache.put(key, bitmap);
    }

    @Override
    public void setImageRemovedListener(ImageRemovedListener listener) {
        this.imageRemovedListener = listener;
    }
}
