package com.bumptech.glide.load.engine.cache;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.Resource;

public class MemoryCacheAdapter implements MemoryCache {

    private ResourceRemovedListener listener;

    @Override
    public void setSizeMultiplier(float multiplier) {
        // Do nothing.
    }

    @Override
    public Resource remove(Key key) {
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
    public void clearMemory() {
        // Do nothing.
    }

    @Override
    public void trimMemory(int level) {
        // Do nothing.
    }
}
