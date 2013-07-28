package com.bumptech.glide.resize.loader;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.Glide;
import com.bumptech.glide.loader.image.BaseImageLoader;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.resize.ImageManager;

/**
 * A base class for loaders that user ImageManager. Primarily responsible for keeping track of bitmaps for recycling
 * purposes.
 */
public abstract class ImageManagerLoader extends BaseImageLoader {

    protected final ImageManager imageManager;
    private Bitmap acquired;
    private Object loadToken;

    public ImageManagerLoader(Context context) {
        this(Glide.get().getImageManager(context));
    }

    public ImageManagerLoader(ImageManager imageManager) {
        this.imageManager = imageManager;
    }
    @Override
    protected final void doFetchImage(String id, StreamLoader streamLoader, int width, int height, ImageReadyCallback cb) {
        loadToken = loadFromImageManager(id, streamLoader, width, height, cb);
    }

    /**
     * An abstract method to make a specific resize call to the {@link ImageManager}
     *
     * @param id A string id that uniquely identifies the image to be loaded. It may include the width and height, but
     *           is not required to do so
     * @param streamLoader The {@link StreamLoader} that will be used to load the image if it is not cached
     * @param width The width of the view where the image will be displayed
     * @param height The height of the view where the image will be displayed
     * @param cb The callback to call when the bitmap is loaded into memory, or when a load fails
     *
     * @return A reference to the fetch that must be retained by the calling object as long as the fetch is relevant
     */
    protected abstract Object loadFromImageManager(String id, StreamLoader streamLoader, int width, int height, ImageReadyCallback cb);

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
