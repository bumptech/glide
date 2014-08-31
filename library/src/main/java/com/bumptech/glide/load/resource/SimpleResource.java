package com.bumptech.glide.load.resource;

import com.bumptech.glide.load.engine.Resource;

/**
 * Simple wrapper for an arbitrary object which helps to satisfy some of the glide engine's contracts.
 * <b>Suggested usages only include resource object which don't have size and cannot be recycled/closed.</b>
 *
 * @param <T> type of the wrapped resource
 */
// TODO: there isn't much point in caching these...
public class SimpleResource<T> extends Resource<T> {
    protected final T data;

    public SimpleResource(T data) {
        this.data = data;
    }

    @Override
    public final T get() {
        return data;
    }

    @Override
    public final int getSize() {
        return 1;
    }

    @Override
    protected void recycleInternal() {
        // no op
    }
}
