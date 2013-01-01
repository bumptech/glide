package com.bumptech.photos.imagemanager.loader;

import android.graphics.Bitmap;
import com.bumptech.photos.loader.image.BaseImageLoader;
import com.bumptech.photos.imagemanager.LoadedCallback;
import com.bumptech.photos.imagemanager.ImageManager;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/1/13
 * Time: 2:51 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class PhotoManagerLoader<T> extends BaseImageLoader<T> {

    protected final ImageManager imageManager;
    private Bitmap acquired;
    private Object loadToken;

    public PhotoManagerLoader(ImageManager imageManager) {
        this.imageManager = imageManager;
    }
    @Override
    protected final void doFetchImage(String path, T model, int width, int height, ImageReadyCallback cb) {
        releaseAcquired();
        loadToken = doFetchImage(path, width, height, new PhotoManagerLoaderCallback(this, cb));
    }

    protected abstract Object doFetchImage(String path, int width, int height, LoadedCallback cb);

    @Override
    protected void onImageReady(Bitmap image, boolean isUsed) {
        if (isUsed) {
            releaseAcquired();
            imageManager.acquireBitmap(image);
            acquired = image;
        } else {
            imageManager.rejectBitmap(image);
        }
    }

    @Override
    public void clear() {
        releaseAcquired();
    }

    private void releaseAcquired() {
        if (acquired != null) {
            imageManager.releaseBitmap(acquired);
            acquired = null;
        }
    }

    protected static class PhotoManagerLoaderCallback extends InternalImageReadyCallback implements LoadedCallback {

        public PhotoManagerLoaderCallback(BaseImageLoader imageLoader, ImageReadyCallback cb) {
            super(imageLoader, cb);
        }

        @Override
        public void onLoadCompleted(Bitmap loaded) {
            onImageReady(loaded);
        }

        @Override
        public void onLoadFailed(Exception e) {
            onError(e);
        }
    }
}
