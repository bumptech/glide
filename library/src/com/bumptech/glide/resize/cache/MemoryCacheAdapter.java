package com.bumptech.glide.resize.cache;

import android.graphics.Bitmap;

public class MemoryCacheAdapter implements MemoryCache {

    @Override
    public boolean contains(String key) {
        return false;
    }

    @Override
    public Bitmap get(String key) {
        return null;
    }

    @Override
    public Bitmap put(String key, Bitmap bitmap) {
        return null;
    }

    @Override
    public void setImageRemovedListener(ImageRemovedListener listener) { }
}
