package com.bumptech.glide.resize.cache;

import com.bumptech.glide.resize.Key;
import com.bumptech.glide.resize.Resource;

public class MemoryCacheAdapter implements MemoryCache {

    private ResourceRemovedListener listener;

    @Override
    public boolean contains(Key key) {
        return false;
    }

    @Override
    public Resource get(Key key) {
        return null;
    }

    @Override
    public Resource put(Key key, Resource resource) {
        listener.onResourceRemoved(resource);
        return null;
    }

    @Override
    public void setResourceRemovedListener(ResourceRemovedListener listener) {
        this.listener = listener;
    }

    @Override
    public void clearMemory() {}

    @Override
    public void trimMemory(int level) {}
}
