package com.bumptech.glide.resize.cache;

import com.bumptech.glide.resize.Resource;

public class MemoryCacheAdapter implements MemoryCache {

    private ResourceRemovedListener listener;

    @Override
    public boolean contains(String key) {
        return false;
    }

    @Override
    public Resource get(String key) {
        return null;
    }

    @Override
    public Resource put(String key, Resource resource) {
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
