package com.bumptech.glide.resize.cache;

import android.graphics.Bitmap;

public class MemoryCacheAdapter implements MemoryCache {
    @Override
    public boolean contains(Integer key) {
        return false;
    }

    @Override
    public Bitmap get(Integer key) {
        return null;
    }

    @Override
    public Bitmap put(Integer key, Bitmap bitmap) {
        return null;
    }

    @Override
    public void setImageRemovedListener(ImageRemovedListener listener) { }
}
