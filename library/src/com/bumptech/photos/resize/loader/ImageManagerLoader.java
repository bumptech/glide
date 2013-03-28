package com.bumptech.photos.resize.loader;

import android.graphics.Bitmap;
import com.bumptech.photos.loader.image.BaseImageLoader;
import com.bumptech.photos.resize.ImageManager;

/**
 * A base class for loaders that user ImageManager. Primarily responsible for keeping track of bitmaps for recycling
 * purposes.
 */
public abstract class ImageManagerLoader<T> extends BaseImageLoader<T> {

    protected final ImageManager imageManager;
    private Bitmap acquired;
    private Object loadToken;

    public ImageManagerLoader(ImageManager imageManager) {
        this.imageManager = imageManager;
    }
    @Override
    protected final void doFetchImage(String path, T model, int width, int height, ImageReadyCallback cb) {
        clear();
        if (path != null) {
            loadToken = doFetchImage(path, width, height, cb);
        }
    }

    protected abstract Object doFetchImage(String path, int width, int height, ImageReadyCallback cb);

    @Override
    protected void onImageReady(String path, T model, Bitmap image, boolean isUsed) {
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
        if (loadToken != null) {
            imageManager.cancelTask(loadToken);
            loadToken = null;
        }
    }

    private void releaseAcquired() {
        if (acquired != null) {
            imageManager.releaseBitmap(acquired);
            acquired = null;
        }
    }

}
