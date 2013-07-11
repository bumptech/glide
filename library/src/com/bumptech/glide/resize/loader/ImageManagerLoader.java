package com.bumptech.glide.resize.loader;

import android.graphics.Bitmap;
import com.bumptech.glide.loader.opener.StreamOpener;
import com.bumptech.glide.loader.image.BaseImageLoader;
import com.bumptech.glide.resize.ImageManager;

/**
 * A base class for loaders that user ImageManager. Primarily responsible for keeping track of bitmaps for recycling
 * purposes.
 */
public abstract class ImageManagerLoader extends BaseImageLoader {

    protected final ImageManager imageManager;
    private Bitmap acquired;
    private Object loadToken;

    public ImageManagerLoader(ImageManager imageManager) {
        this.imageManager = imageManager;
    }
    @Override
    protected final void doFetchImage(String id, StreamOpener streamOpener, int width, int height, ImageReadyCallback cb) {
        clear();
        if (streamOpener != null) {
            loadToken = loadFromImageManager(id, streamOpener, width, height, cb);
        }
    }

    protected abstract Object loadFromImageManager(String id, StreamOpener streamOpener, int width, int height, ImageReadyCallback cb);

    @Override
    protected void onImageReady(String id, Bitmap image, boolean isUsed) {
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
