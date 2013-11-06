/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.resize.cache;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE;

import android.graphics.Bitmap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

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

    @Override
    public void clearMemory() {
        final Iterator<Map.Entry<String,Bitmap>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            final Bitmap bitmap = iterator.next().getValue();
            bitmap.recycle();
            iterator.remove();
        }
        currentSize = 0;
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
            currentSize -= getSize(toRemove);
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
