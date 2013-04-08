package com.bumptech.photos.loader.image;

import android.graphics.Bitmap;

import java.lang.ref.WeakReference;


/**
 * A base class for {@link ImageLoader} that provides some lifecycle methods and prevents memory leaks by only providing
 * subclasses with a weak reference to the calling object.
 *
 * @param <T> The type of the model this loader must be able to load a {@link android.graphics.Bitmap} for
 */
public abstract class BaseImageLoader<T> implements ImageLoader<T> {
    @Override
    public final Object fetchImage(String path, T model, int width, int height, ImageReadyCallback cb) {
        doFetchImage(path, model, width, height, new InternalImageReadyCallback(cb, path, model));
        return cb;
    }

    @Override
    public void clear() { }

    /**
     * The method where subclasses should actually begin any long running load for the given path and model. It is
     * safe to strongly reference the given callback since that callback only weakly references the object that created
     * it. Once a load completes or fails the given callback should be called to signal to the calling object that the
     * image is ready.
     *
     * @see ImageLoader#fetchImage(String, Object, int, int, com.bumptech.photos.loader.image.ImageLoader.ImageReadyCallback)
     *
     * @param path The path to the image or null if the required information is contained in the model
     * @param model The object that represents or contains an image that can be displayed
     * @param width The width of the view where the image will be displayed
     * @param height The height of the view where the image will be displayed
     * @param cb The callback to call when the bitmap is loaded into memory, or when a load fails
     */
    protected abstract void doFetchImage(String path, T model, int width, int height, ImageReadyCallback cb);

    /**
     * A lifecycle method called after the requesting object is notified that this loader has loaded a bitmap. Should be
     * used to cleanup or update any data related to the completed load. Should not be used as a callback to change how
     * an image is displayed. See {@link com.bumptech.photos.presenter.ImageSetCallback} instead to make a visual change
     * when a load completes.
     *
     * @param path The path to the loaded image
     * @param model The model representing the loaded image
     * @param image The loaded image
     * @param isUsed True iff the requesting object is going to display the image
     */
    protected void onImageReady(String path, T model, Bitmap image, boolean isUsed) { }

    /**
     * A lifecycle method called after the requesting object is notified that this loader failed to loada Bitmap. Should
     * be used to cleanup or update any data related to the failed load.
     *
     * @param e The exception that caused the failure, or null
     * @param model The model representing the image this loader failed to load
     * @param path The path to the image this loader failed to load
     * @return True iff this image loader has handled the exception and the cb should not be notified.
     */
    protected boolean onImageLoadFailed(Exception e, T model, String path) {
        return false;
    }

    protected class InternalImageReadyCallback implements ImageReadyCallback {
        private final WeakReference<ImageReadyCallback> cbRef;
        private final String path;
        private final WeakReference<T> modelRef;

        public InternalImageReadyCallback(ImageReadyCallback cb, String path, T model) {
            this.cbRef = new WeakReference<ImageReadyCallback>(cb);
            this.modelRef = new WeakReference<T>(model);
            this.path = path;
        }

        @Override
        public final boolean onImageReady(Bitmap image) {
            final ImageReadyCallback cb = cbRef.get();
            final T model = modelRef.get();
            boolean result = false;
            if (cb != null && modelRef != null) {
                result = cb.onImageReady(image);
                BaseImageLoader.this.onImageReady(path, model, image, result);
            }
            return result;
        }

        @Override
        public void onException(Exception e) {
            final ImageReadyCallback cb = cbRef.get();
            final T model = modelRef.get();
            if (cb != null && model != null) {
                if (!BaseImageLoader.this.onImageLoadFailed(e, model, path)) {
                    cb.onException(e);
                }
            }
        }
    }
}
