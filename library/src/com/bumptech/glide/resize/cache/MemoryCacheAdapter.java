package com.bumptech.glide.resize.cache;

import android.graphics.Bitmap;

public class MemoryCacheAdapter implements MemoryCache {

    private ImageRemovedListener listener;

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
        listener.onImageRemoved(bitmap);
        return null;
    }

    @Override
    public void setImageRemovedListener(ImageRemovedListener listener) {
        this.listener = listener;
    }
}
