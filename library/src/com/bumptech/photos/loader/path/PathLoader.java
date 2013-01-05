/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.loader.path;

/**
 * An interface used by {@link com.bumptech.photos.presenter.ImagePresenter} to fetch a path for a given model
 *
 * @param <T> The type of the model this loader must be able to fetch paths for
 */
public interface PathLoader<T> {

    /**
     * An interface defining a callback that will be passed to a {@link PathLoader} and that should be called by the
     * {@link PathLoader} when a load completes either successfully or because of a failure
     */
    public interface PathReadyCallback {

        /**
         * The method a loader should call when a load completes successfully
         *
         * @param path The requested path
         * @return True iff the loaded path was used by the class that requested it from the {@link PathLoader}
         */
        public boolean onPathReady(String path);

        /**
         * The method a loader should call when a load fails
         *
         * @param e The exception that caused the load to fail, or null
         */
        public void onError(Exception e);
    }

    /**
     * Load the path represented by the given model
     *
     * @param model The object that represents or contains the path to an image.
     * @param width The width of the view where the image will be displayed
     * @param height The height of the view where the image will be displayed
     * @param cb The callback to call when the path is loaded or when a load fails
     *
     * @return A reference to the fetch that must be retained by the calling object as long as the fetch is relevant
     */
    public Object fetchPath(T model, int width, int height, PathReadyCallback cb);

    /**
     * Called when the current path load does not need to conintue and any corresponding cleanup to save cpu or memory
     * can be done. Will not be called if a load completes successfully.
     */
    public void clear();
}
