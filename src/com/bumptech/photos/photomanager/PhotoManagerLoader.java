package com.bumptech.photos.photomanager;

import android.graphics.Bitmap;
import com.bumptech.photos.photomanager.LoadedCallback;
import com.bumptech.photos.photomanager.PhotoManager;
import com.bumptech.photos.view.loader.BaseImageLoader;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/1/13
 * Time: 2:51 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class PhotoManagerLoader<T> extends BaseImageLoader<T> {

    protected final PhotoManager photoManager;
    private Bitmap acquired;
    private Object loadToken;

    public PhotoManagerLoader(PhotoManager photoManager) {
        this.photoManager = photoManager;
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
            photoManager.acquireBitmap(image);
            acquired = image;
        } else {
            photoManager.rejectBitmap(image);
        }
    }

    @Override
    public void clear() {
        releaseAcquired();
    }

    private void releaseAcquired() {
        if (acquired != null) {
            photoManager.releaseBitmap(acquired);
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
