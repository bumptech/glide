package com.bumptech.photos.loader.path;

import java.lang.ref.WeakReference;

/**
 * A base class for {@link PathLoader that provides some lifecycle methods and prevents memory leaks by only providing
 * subclasses with a weak reference to the callinb object.}
 *
 * @param <T> The type of the model this loader must be able to load a path for
 */
public abstract class BasePathLoader<T> implements PathLoader<T> {
    @Override
    public final Object fetchPath(T model, int width, int height, PathReadyCallback cb) {
        doFetchPath(model, width, height, new InternalPathReadyCallback(cb, model));
        return cb;
    }

    @Override
    public void clear() { }

    /**
     * The method where subclasses should actually begin any long running load to fetch the path from the given model.
     * It is safe to strongly reference the given callback since that callback only weakly references the object that
     * created it. Once a load completes or fails the given callback should be called to signal to the calling object
     * that the path is ready.
     *
     * @see com.bumptech.photos.loader.image.ImageLoader#fetchImage(String, Object, int, int, com.bumptech.photos.loader.image.ImageLoader.ImageReadyCallback)
     *
     * @param model The object that represents or contains a path to an image to be displayed
     * @param width The width of the view where the image will be displayed
     * @param height The height of the view where the image will be displayed
     * @param cb The callback to call when the bitmap is loaded into memory, or when a load fails
     */
    protected abstract void doFetchPath(T model, int width, int height, PathReadyCallback cb);

    /**
     * A lifecycle method called after the requesting object is notified that this loader has loaded a path. Should be
     * used to cleanup or update any data related ot the completed load.
     *
     * @param path The retrieved path to the image
     * @param model The model representing the image
     * @param isUsed True iff the requesting object is going to load the image at the given path
     */
    protected void onPathReady(String path, T model, boolean isUsed) { }

    /**
     * A lifecycle method called after the requesting object is notified that this loader failed to load a Bitmap.
     * SHould be used to cleanup or update any data related to the failed load.
     *
     * @param model The model representing the image this loader failed to fetch a path for
     * @param e The exception that caused the failure, or null
     */
    protected void onPathFetchFailed(T model, Exception e) { }

    protected class InternalPathReadyCallback implements PathReadyCallback{
        private final WeakReference<PathReadyCallback> cbRef;
        private final WeakReference<T> modelRef;

        public InternalPathReadyCallback(PathReadyCallback cb, T model) {
            this.cbRef = new WeakReference<PathReadyCallback>(cb);
            this.modelRef = new WeakReference<T>(model);
        }

        @Override
        public final boolean onPathReady(String path) {
            final PathReadyCallback cb = cbRef.get();
            final T model = modelRef.get();
            boolean result = false;
            if (cb != null && model != null) {
                result = cb.onPathReady(path);
                BasePathLoader.this.onPathReady(path, model, result);
            }
            return result;
        }

        @Override
        public final void onError(Exception e) {
            final PathReadyCallback cb = cbRef.get();
            final T model = modelRef.get();
            if (cb != null && model != null) {
                cb.onError(e);
                BasePathLoader.this.onPathFetchFailed(model, e);
            }
        }
    }

}
