package com.bumptech.glide.loader.stream;

import java.io.InputStream;

/**
 * A base class for lazily and asynchronously opening an input stream that can be used to load an image.
 * A new instance is created per image load by {@link com.bumptech.glide.loader.model.ModelLoader}. loadStream
 * may or may not be called for any given load depending on whether or not the corresponding image is cached. Cancel
 * also may or may not be called.
 */
public interface StreamLoader {

    /**
     * An interface defining a callback for when an InputStream has been opened successfully or the load
     * has failed because of an exception. Any checked exceptions that prevent the InputStream from being opened
     * should call {@link #onException(Exception)} and should not call {@code e.printStackTrace()} to avoid the
     * stack trace being printed twice.
     */
    public interface StreamReadyCallback {
        /**
         * The method that should be called when a load completes successfully
         *
         * @param is The {@link InputStream} that will be used to load the image
         */
        public void onStreamReady(InputStream is);

        /**
         * The method that should be called when a load fails because of an exception. This may or may not
         * be called on the main thread.
         *
         * @param e The exception that caused the load to fail
         */
        public void onException(Exception e);

    }

    /**
     * Asynchronously fetch and open an InputStream representing an image. This will always be called on
     * background thread so it is safe to perform long running tasks here. Any third party libraries called
     * must be thread safe since this method will be called from a thread in a
     * {@link java.util.concurrent.ExecutorService} that may have more than one background thread.
     *
     * This method will only be called when the corresponding image is not in the cache.
     *
     * @param cb The callback to call when the load completes or fails due to an exception
     */
    public void loadStream(StreamReadyCallback cb);

    /**
     * A method that will be called by an {@link com.bumptech.glide.presenter.ImagePresenter} when a load is no longer
     * relevant (because we now want to load a different image into the view). This method does not need to guarantee
     * that any in process loads do not finish. It also may be called before a load starts or after it finishes.
     *
     * The best way to use this method is to cancel any loads that have not yet started, but allow those that are in
     * process to finish since its we typically will want to display the same image in a different view in
     * the near future.
     */
    public void cancel();
}
