/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.resize.cache;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE;

import android.graphics.Bitmap;
import com.bumptech.glide.util.LruCache;
import com.bumptech.glide.util.Util;

public class LruMemoryCache extends LruCache<String, Bitmap> implements MemoryCache {
    private ImageRemovedListener imageRemovedListener;

    public LruMemoryCache(int size) {
        super(size);
    }

    @Override
    protected int getSize(Bitmap item) {
        return Util.getSize(item);
    }

    @Override
    protected void onItemRemoved(Bitmap item) {
        if (imageRemovedListener != null) {
            imageRemovedListener.onImageRemoved(item);
        }
    }

    @Override
    public void setImageRemovedListener(ImageRemovedListener listener) {
        this.imageRemovedListener = listener;
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
            trimToSize(getCurrentSize() / 2);
        }
    }
}
