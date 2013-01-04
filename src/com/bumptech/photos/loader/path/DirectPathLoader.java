/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.loader.path;

/**
 * A simple synchronous implementation of a {@link PathLoader}
 *
 * @param <T> The type of the model that contains a path
 */
public abstract class DirectPathLoader<T> implements PathLoader<T> {

    @Override
    public final Object fetchPath(T model, int width, int height, PathReadyCallback cb) {
        cb.onPathReady(getPath(model, width, height));
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * A method to be implemented by subclasses that should return a path for a given model (or null if the model
     * contains some other mechanism to load the image directly)
     *
     * @param model The object containing the path
     * @param width The width of the {@link android.widget.ImageView} that will be displaying the image
     * @param height The height of the {@link android.widget.ImageView} that will be displaying the image
     * @return The path where the image is located, or null
     */
    protected abstract String getPath(T model, int width, int height);

    @Override
    public final void clear() { }
}
