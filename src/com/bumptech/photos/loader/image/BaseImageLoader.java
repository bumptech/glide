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
        doFetchImage(path, model, width, height, cb);
        return cb;
    }

    @Override
    public void clear() { }

    protected abstract void doFetchImage(String path, T model, int width, int height, ImageReadyCallback cb);

    protected void onImageReady(Bitmap image, boolean isUsed) { }

    protected void onImageLoadFailed(Exception e) { }


    protected static class InternalImageReadyCallback {
        private final WeakReference<ImageReadyCallback> cbRef;
        private final WeakReference<BaseImageLoader> imageLoaderRef;

        public InternalImageReadyCallback(BaseImageLoader imageLoader, ImageReadyCallback cb) {
            this.imageLoaderRef = new WeakReference<BaseImageLoader>(imageLoader);
            this.cbRef = new WeakReference<ImageReadyCallback>(cb);
        }

        protected final void onImageReady(Bitmap image) {
            final BaseImageLoader imageLoader = imageLoaderRef.get();
            final ImageReadyCallback cb = cbRef.get();
            if (imageLoader != null && cb != null) {
                imageLoader.onImageReady(image, cb.onImageReady(image));
            }
        }

        protected final void onError(Exception e) {
            final BaseImageLoader imageLoader = imageLoaderRef.get();
            final ImageReadyCallback cb = cbRef.get();
            if (imageLoader != null && cb != null) {
                cb.onError(e);
                imageLoader.onImageLoadFailed(e);
            }
        }
    }
}
