package com.bumptech.glide.loader.image;

import android.graphics.Bitmap;
import com.bumptech.glide.loader.stream.StreamLoader;

import java.lang.ref.WeakReference;


/**
 * A base class for {@link ImageLoader} that provides some lifecycle methods and prevents memory leaks by only providing
 * subclasses with a weak reference to the calling {@link com.bumptech.glide.presenter.ImagePresenter}.
 */
public abstract class BaseImageLoader implements ImageLoader {
    @Override
    public final Object fetchImage(String id, StreamLoader streamLoader, int width, int height, ImageReadyCallback cb) {
        doFetchImage(id, streamLoader, width, height, new InternalImageReadyCallback(cb, id));
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
     * @see ImageLoader#fetchImage(String, com.bumptech.glide.loader.stream.StreamLoader, int, int, com.bumptech.glide.loader.image.ImageLoader.ImageReadyCallback)
     *
     * @param id A string id that uniquely identifies the image to be loaded. It may include the width and height, but
     *           is not required to do so
     * @param streamLoader The {@link StreamLoader} that will be used to load the image if it is not cached
     * @param width The width of the view where the image will be displayed
     * @param height The height of the view where the image will be displayed
     * @param cb The callback to call when the bitmap is loaded into memory, or when a load fails
     */
    protected abstract void doFetchImage(String id, StreamLoader streamLoader, int width, int height, ImageReadyCallback cb);

    /**
     * A lifecycle method called after the requesting object is notified that this loader has loaded a bitmap. Should be
     * used to cleanup or update any data related to the completed load. Should not be used as a callback to change how
     * an image is displayed. See {@link com.bumptech.glide.presenter.ImageSetCallback} instead to make a visual change
     * when a load completes.
     *
     * @param id The unique id of the image
     * @param image The loaded image
     * @param isUsed True iff the requesting object is going to display the image
     */
    protected void onImageReady(String id, Bitmap image, boolean isUsed) { }

    /**
     * A lifecycle method called after the requesting object is notified that this loader failed to load a Bitmap.
     * Should be used to cleanup or update any data related to the failed load.
     *
     * @param e The exception that caused the failure, or null
     * @param id The unique id of the image
     * @return True iff this image loader has handled the exception and the cb should not be notified.
     */
    @SuppressWarnings("unused")
    protected boolean onImageLoadFailed(Exception e, String id) {
        return false;
    }

    protected class InternalImageReadyCallback implements ImageReadyCallback {
        private final WeakReference<ImageReadyCallback> cbRef;
        private final String id;

        public InternalImageReadyCallback(ImageReadyCallback cb, String id) {
            this.cbRef = new WeakReference<ImageReadyCallback>(cb);
            this.id = id;
        }

        @Override
        public final boolean onImageReady(Bitmap image) {
            final ImageReadyCallback cb = cbRef.get();
            boolean result = false;
            if (cb != null) {
                result = cb.onImageReady(image);
                BaseImageLoader.this.onImageReady(id, image, result);
            }
            return result;
        }

        @Override
        public void onException(Exception e) {
            final ImageReadyCallback cb = cbRef.get();
            if (cb != null) {
                if (!BaseImageLoader.this.onImageLoadFailed(e, id)) {
                    cb.onException(e);
                }
            }
        }
    }
}
