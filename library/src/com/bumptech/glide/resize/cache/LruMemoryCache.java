/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.resize.cache;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE;

import android.graphics.Bitmap;
import com.bumptech.glide.util.Util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 */
public class LruMemoryCache implements MemoryCache {
    private final LinkedHashMap<String, Bitmap> cache = new LinkedHashMap<String, Bitmap>(15, 0.75f, true);
    private final int maxSize;
    private ImageRemovedListener imageRemovedListener;
    private int currentSize = 0;

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
        currentSize += Util.getSize(bitmap);
        final Bitmap result = cache.put(key, bitmap);
        evict();
        return result;
    }

    @Override
    public void setImageRemovedListener(ImageRemovedListener listener) {
        this.imageRemovedListener = listener;
    }

    @Override
    public void clearMemory() {
        trimToSize(0);
    }

    @Override
    public void trimMemory(int level) {
        if (level >= TRIM_MEMORY_MODERATE) {
            // Nearing middle of list of cached background apps
            // Evict our entire bitmap cache
            clearMemory();
        } else if (level >= TRIM_MEMORY_BACKGROUND) {
            // Entering list of cached background apps
            // Evict oldest half of our bitmap cache
            trimToSize(currentSize / 2);
        }
    }

    private void trimToSize(int size) {
        Map.Entry<String, Bitmap> last;
        while (currentSize > size) {
            last = cache.entrySet().iterator().next();
            final Bitmap toRemove = last.getValue();
            currentSize -= Util.getSize(toRemove);
            cache.remove(last.getKey());

            if (imageRemovedListener != null) {
                imageRemovedListener.onImageRemoved(toRemove);
            }
        }
    }

    private void evict() {
        trimToSize(maxSize);
    }
}
