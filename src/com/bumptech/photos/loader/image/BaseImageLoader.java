package com.bumptech.photos.loader.image;

import android.graphics.Bitmap;

import java.lang.ref.WeakReference;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/1/13
 * Time: 2:45 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseImageLoader<T> implements ImageLoader<T> {
    @Override
    public final Object fetchImage(String path, T model, int width, int height, ImageReadyCallback cb) {
        doFetchImage(path, model, width, height, new InternalImageReadyCallback(cb, path, model));
        return cb;
    }

    @Override
    public void clear() { }

    protected abstract void doFetchImage(String path, T model, int width, int height, ImageReadyCallback cb);

    protected void onImageReady(String path, T model, Bitmap image, boolean isUsed) { }

    protected void onImageLoadFailed(String path, T model, Exception e) { }


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
        public void onError(Exception e) {
            final ImageReadyCallback cb = cbRef.get();
            final T model = modelRef.get();
            if (cb != null && model != null) {
                cb.onError(e);
                BaseImageLoader.this.onImageLoadFailed(path, model, e);
            }
        }
    }
}
