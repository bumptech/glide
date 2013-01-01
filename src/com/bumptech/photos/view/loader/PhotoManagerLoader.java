/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.view.loader;

import android.graphics.Bitmap;
import com.bumptech.photos.LoadedCallback;
import com.bumptech.photos.PhotoManager;
import com.bumptech.photos.view.assetpath.AssetPathConverter;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/28/12
 * Time: 9:53 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class PhotoManagerLoader<T> extends BaseImageLoader<T> {

    protected final PhotoManager photoManager;
    private final AssetPathConverter<T> assetToPath;
    private Bitmap acquired = null;
    private Object loadToken = null;

    public PhotoManagerLoader(PhotoManager photoManager, AssetPathConverter<T> assetToPath) {
        this.photoManager = photoManager;
        this.assetToPath = assetToPath;
    }

    @Override
    public final void doFetchPath(T model, int width, int height, ImageLoader.PathReadyCallback cb) {
        assetToPath.fetchPath(model, width, height, new PathReadyCallback(this, cb));
    }

    @Override
    protected final void doFetchImage(String path, T model, int width, int height, ImageLoader.ImageReadyCallback cb) {
        if (loadToken != null)  {
            photoManager.cancelTask(loadToken);
        }
        if (acquired != null) {
            photoManager.releaseBitmap(acquired);
            acquired = null;
        }

        loadToken = doFetchImage(path, model, width, height, new ImageReadyCallback(this, cb));
    }

    protected abstract Object doFetchImage(String path, T model, int width, int height, LoadedCallback cb);

    @Override
    protected void onImageReady(Bitmap image, boolean isUsed) {
        if (isUsed) {
            if (acquired != null) {
                photoManager.releaseBitmap(acquired);
            }
            photoManager.acquireBitmap(image);
            acquired = image;
        } else {
            photoManager.rejectBitmap(image);
        }
    }

    @Override
    public void onImageLoadFailed(Exception e, ImageLoader.ImageReadyCallback cb) {
        cb.onError(e);
    }

    @Override
    public void clear() {
        if (acquired != null) {
            photoManager.releaseBitmap(acquired);
            acquired = null;
        }
    }

    private static class PathReadyCallback<T> extends BaseImageLoader.InternalPathReadyCallback<T> implements AssetPathConverter.PathReadyListener {

        public PathReadyCallback(BaseImageLoader<T> imageLoader, ImageLoader.PathReadyCallback cb) {
            super(imageLoader, cb);
        }

        @Override
        public void pathReady(String path) {
            onPathReady(path);
        }

        @Override
        public void onError(Exception e) {
            onPathLoadFailed(e);
        }
    }

    private static class ImageReadyCallback<T> extends BaseImageLoader.InternalImageReadyCallback<T> implements LoadedCallback{

        public ImageReadyCallback(BaseImageLoader<T> imageLoader, ImageLoader.ImageReadyCallback cb) {
            super(imageLoader, cb);
        }

        @Override
        public void onLoadCompleted(Bitmap loaded) {
            onImageReady(loaded);
        }

        @Override
        public void onLoadFailed(Exception e) {
            onImageLoadFailed(e);
        }
    }

}
