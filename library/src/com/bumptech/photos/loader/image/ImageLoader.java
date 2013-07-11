package com.bumptech.photos.loader.image;

import android.graphics.Bitmap;
import com.bumptech.photos.loader.opener.StreamOpener;

/**
 * An interface used by {@link com.bumptech.photos.presenter.ImagePresenter} to fetch a bitmap for a given id and
 * dimensions and/or input streams
 *
 */
public interface ImageLoader {

    /**
     * An interface defining a callback that will be passed to an {@link ImageLoader} and that should be called by the
     * {@link ImageLoader} when a load completes either successfully or because of an exception
     */
    public interface ImageReadyCallback {

        /**
         * The method a loader should call when a load completes successfully
         *
         * @param image The requested image
         * @return True iff the loaded image was used by the class that requested it from the {@link ImageLoader}
         */
        public boolean onImageReady(Bitmap image);

        /**
         * The method a loader should call when a load fails
         *
         * @param e The exception that caused the load to fail, or null
         */
        public void onException(Exception e);
    }

    /**
     * Load the image at the given path represented by the given model
     *
     * @param id A unique id identifying this particular image that will be combined with the provided size info to use as a cache key.
     * @param streamOpener The {@link StreamOpener} that will be used to load the image if it is not cached
     * @param width The width of the view where the image will be displayed
     * @param height The height of the view where the image will be displayed
     * @param cb The callback to call when the bitmap is loaded into memory, or when a load fails
     *
     * @return A reference to the fetch that must be retained by the calling object as long as the fetch is relevant
     */
    public Object fetchImage(String id, StreamOpener streamOpener, int width, int height, ImageReadyCallback cb);

    /**
     * Called when the current image load does not need to continue and any corresponding cleanup to save cpu
     * or memory can be done. Will not be called if a load completes successfully.
     */
    public void clear();
}
