package com.bumptech.glide.load.engine.cache;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.EngineResource;

/**
 * A simple class that ignores all puts and returns null for all gets.
 */
public class MemoryCacheAdapter implements MemoryCache {

    private ResourceRemovedListener listener;

    @Override
    public void setSizeMultiplier(float multiplier) {
        // Do nothing.
    }

    @Override
    public EngineResource<?> remove(Key key) {
        return null;
    }

    @Override
    public EngineResource<?> put(Key key, EngineResource<?> resource) {
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
