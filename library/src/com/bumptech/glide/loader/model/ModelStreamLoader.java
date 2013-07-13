package com.bumptech.glide.loader.model;

import com.bumptech.glide.loader.opener.StreamOpener;

/**
 * An interface used by {@link com.bumptech.glide.presenter.ImagePresenter} to fetch a unique id and a means of
 * obtaining input streams to an image represented by the given model
 *
 * @param <T> The type of the model that represents an image
 */
public interface ModelStreamLoader<T> {
    /**
     * An interface defining a callback that will be passed to an {@link ModelStreamLoader}
     * and that should be called by the {@link ModelStreamLoader} when a load completes either successfully or
     * because of an exception
     */
    public interface ModelStreamReadyCallback {
        /**
         * The method a loader should call when a load completes successfully
         *
         * @param id A unique id identifying this particular image that will be combined with the provided size info to use as a cache key.
         * @param streamOpener The {@link StreamOpener} that will be used to load the image if it is not cached
         * @return True iff the loaded streamOpener and id was used by the class that requested
         */
        public boolean onStreamReady(String id, StreamOpener streamOpener);

        /**
         * The method a loader should call when a load fails
         *
         * @param e The exception that caused the load to fail, or null
         */
        public void onException(Exception e);
    }

    /**
     * Obtain an id and {@link StreamOpener} represented by the given model at the given dimension
     *
     * @param model The model representing the image to be loaded
     * @param width The width of the view the image will be displayed in
     * @param height The height of the view the image will be displayed in
     * @param cb The callback to call when the load completes
     *
     * @return A reference to the fetch that must be retained by the calling object as long as the fetch is relavent
     */
    public Object fetchModelStream(T model, int width, int height, ModelStreamReadyCallback cb);

    /**
     * Called when the current load does not need to continue and any corresponding cleanup to save cpu or memory can be
     * done. Will not be called if a load completes successfully.
     */
    public void clear();
}
