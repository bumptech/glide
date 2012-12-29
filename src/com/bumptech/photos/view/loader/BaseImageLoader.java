/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.view.loader;

import android.graphics.Bitmap;

import java.lang.ref.WeakReference;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/29/12
 * Time: 2:05 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseImageLoader<T> implements ImageLoader<T> {

    @Override
    public final Object fetchPath(T model, int width, int height, PathReadyCallback cb) {
        doFetchPath(model, width, height, cb);
        return cb;
    }

    protected abstract void doFetchPath(T model, int width, int height, PathReadyCallback cb);

    private final void onPathReady(String path, PathReadyCallback cb) {
        onPathReady(path, cb.onPathReady(path));
    }

    protected void onPathReady(String path, boolean isUsed) {}

    protected void onPathFetchFailed(Exception e, PathReadyCallback cb) {
        cb.onError(e);
    }

    @Override
    public final Object fetchImage(String path, T model, int width, int height, ImageReadyCallback cb) {
        doFetchImage(path, model, width, height, cb);
        return cb;
    }

    protected abstract void doFetchImage(String path, T model, int width, int height, ImageReadyCallback cb);

    private final void onImageReady(Bitmap image, ImageReadyCallback cb) {
        onImageReady(image, cb.onImageReady(image));
    }

    protected void onImageReady(Bitmap image, boolean isUsed) {}

    protected void onImageLoadFailed(Exception e, ImageReadyCallback cb) {
        cb.onError(e);
    }

    public abstract static class ImageLoaderCallback<T> {
        private final WeakReference<BaseImageLoader<T>> imageLoaderRef;

        public ImageLoaderCallback(BaseImageLoader<T> imageLoader) {
            imageLoaderRef = new WeakReference<BaseImageLoader<T>>(imageLoader);
        }

        public BaseImageLoader<T> getImageLoader() {
            return imageLoaderRef.get();
        }
    }

    public abstract static class InternalPathReadyCallback<T> extends ImageLoaderCallback<T>{
        private final WeakReference<PathReadyCallback> cbRef;

        public InternalPathReadyCallback(BaseImageLoader<T> imageLoader, PathReadyCallback cb) {
            super(imageLoader);
            this.cbRef = new WeakReference<PathReadyCallback>(cb);
        }

        protected final void onPathReady(String path) {
            final BaseImageLoader<T> imageLoader = getImageLoader();
            final PathReadyCallback cb = cbRef.get();
            if (imageLoader != null && cb != null) {
                imageLoader.onPathReady(path, cb);
            }
        }

        protected final void onPathLoadFailed(Exception e) {
            final BaseImageLoader<T> imageLoader = getImageLoader();
            final PathReadyCallback cb = cbRef.get();
            if (imageLoader != null && cb != null) {
                imageLoader.onPathFetchFailed(e, cb);
            }
        }
    }

    public abstract static class InternalImageReadyCallback<T> extends ImageLoaderCallback<T>{
        private final WeakReference<ImageReadyCallback> cbRef;

        public InternalImageReadyCallback(BaseImageLoader<T> imageLoader, ImageReadyCallback cb) {
            super(imageLoader);
            this.cbRef = new WeakReference<ImageReadyCallback>(cb);
        }

        protected final void onImageReady(Bitmap image) {
            final BaseImageLoader<T> imageLoader = getImageLoader();
            final ImageReadyCallback cb = cbRef.get();
            if (imageLoader != null && cb != null) {
                imageLoader.onImageReady(image, cb);
            }
        }

        protected final void onImageLoadFailed(Exception e) {
            final BaseImageLoader<T> imageLoader = getImageLoader();
            final ImageReadyCallback cb = cbRef.get();
            if (imageLoader != null && cb != null) {
                imageLoader.onImageLoadFailed(e, cb);
            }
        }
    }

}
