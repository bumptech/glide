/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.resize.cache;

import android.graphics.Bitmap;

import java.util.LinkedHashMap;

/**
 */
public class LruMemoryCache implements MemoryCache {
    private final LinkedHashMap<String, Bitmap> cache = new LinkedHashMap<String, Bitmap>(15, 0.75f, true);
    private final int maxSize;
    private ImageRemovedListener imageRemovedListener;
    private int currentSize = 0;

    private static int getSize(Bitmap bitmap) {
        return bitmap.getHeight() * bitmap.getRowBytes();
    }

    public LruMemoryCache(int size) {
        this.maxSize = size;
    }

    @Override
    public boolean contains(String key) {
        return cache.get(key) != null;
    }

    @Override
    public Bitmap get(String key) {
        return cache.get(key);
    }

    @Override
    public Bitmap put(String key, Bitmap bitmap) {
        currentSize += getSize(bitmap);
        final Bitmap result = cache.put(key, bitmap);
        evict();
        return result;
    }

    @Override
    public void setImageRemovedListener(ImageRemovedListener listener) {
        this.imageRemovedListener = listener;
    }

    private void evict() {
        LinkedHashMap.Entry<String, Bitmap> last;
        while (currentSize > maxSize) {
            last = cache.entrySet().iterator().next();
            final Bitmap toRemove = last.getValue();
            currentSize -= getSize(toRemove);
            cache.remove(last.getKey());

            if (imageRemovedListener != null) {
                imageRemovedListener.onImageRemoved(toRemove);
            }
        }
    }
}
