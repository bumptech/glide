package com.bumptech.glide.load.engine.cache;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.Resource;

public class MemoryCacheAdapter implements MemoryCache {

    private ResourceRemovedListener listener;

    @Override
    public void setSizeMultiplier(float multiplier) {
    }

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
