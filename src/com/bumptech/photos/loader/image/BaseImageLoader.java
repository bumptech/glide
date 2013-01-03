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
        doFetchImage(path, model, width, height, new InternalImageReadyCallback(cb));
        return cb;
    }

    @Override
    public void clear() { }

    protected abstract void doFetchImage(String path, T model, int width, int height, ImageReadyCallback cb);

    protected void onImageReady(Bitmap image, boolean isUsed) { }

    protected void onImageLoadFailed(Exception e) { }


    protected class InternalImageReadyCallback implements ImageReadyCallback {
        private final WeakReference<ImageReadyCallback> cbRef;

        public InternalImageReadyCallback(ImageReadyCallback cb) {
            this.cbRef = new WeakReference<ImageReadyCallback>(cb);
        }

        @Override
        public final boolean onImageReady(Bitmap image) {
            final ImageReadyCallback cb = cbRef.get();
            boolean result = false;
            if (cb != null) {
                result = cb.onImageReady(image);
                BaseImageLoader.this.onImageReady(image, result);
            }
            return result;
        }

        @Override
        public void onError(Exception e) {
            final ImageReadyCallback cb = cbRef.get();
            if (cb != null) {
                cb.onError(e);
                BaseImageLoader.this.onImageLoadFailed(e);
            }
        }
    }
}
