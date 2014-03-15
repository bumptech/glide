package com.bumptech.glide.loader.image;

import android.graphics.Bitmap;
import com.bumptech.glide.resize.BitmapLoadTask;

/**
 * An interface used by {@link com.bumptech.glide.presenter.ImagePresenter} to load a bitmap.
 */
public interface ImageLoader {

    /**
     * An interface defining a callback that will be passed to an {@link ImageLoader} and that should be called by the
     * {@link ImageLoader} when a load completes either successfully or because of an exception.
     */
    public interface ImageReadyCallback {

        /**
         * The method a loader should call when a load completes successfully.
         *
         * @param image The requested image.
         * @return True iff the loaded image was used by the class that requested it from the {@link ImageLoader}.
         */
        public boolean onImageReady(Bitmap image);

        /**
         * The method a loader should call when a load fails.
         *
         * @param e The exception that caused the load to fail, or null.
         */
        public void onException(Exception e);
    }

    /**
     * Loads the image using the given load task.
     *
     * @param loadTask The {@link BitmapLoadTask} that defines the image and decoder to use to retrieve a bitmap if
     *                 the image is not cached.
     * @param cb The callback to call when the bitmap is loaded into memory, or when a load fails.
     *
     * @return A reference to the fetch that must be retained by the calling object as long as the fetch is relevant.
     */
    public Object fetchImage(BitmapLoadTask loadTask, ImageReadyCallback cb);

    /**
     * Called when the current image load does not need to continue and any corresponding cleanup to save cpu
     * or memory can be done. Will not be called if a load completes successfully.
     */
    public void clear();
}
