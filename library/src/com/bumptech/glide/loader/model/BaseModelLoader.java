package com.bumptech.glide.loader.model;

import com.bumptech.glide.loader.stream.StreamLoader;

/**
 * A base class that handles canceling any existing {@link StreamLoader} when
 * cleared
 *
 * @param <T> The type of model for this loader
 */
public abstract class BaseModelLoader<T> implements ModelLoader<T> {
    private StreamLoader current;

    @Override
    public final StreamLoader getStreamLoader(T model, int width, int height) {
        current = buildStreamOpener(model, width, height);
        return current;
    }

    protected abstract StreamLoader buildStreamOpener(T model, int width, int height);

    @Override
    public void clear() {
        if (current != null) {
            current.cancel();
            current = null;
        }
    }
}
