package com.bumptech.photos.resize.loader;

import android.graphics.Bitmap;
import com.bumptech.photos.resize.ImageManager;
import com.bumptech.photos.resize.LoadedCallback;

/**
 * An ImageLoader implementation that loads an image at the given path at its original dimensions. Should be used
 * when an image is roughly the same size as the view that will display it and you want to use some external process
 * (like the view) to do the resizing for you. Not memory efficient and more expensive to use recycled Bitmaps for than
 * other implementations
 *
 * @see ImageManager#getImage(String, com.bumptech.photos.resize.LoadedCallback)
 */
public class AsIs<T> extends ImageManagerLoader<T> {

    public AsIs(ImageManager imageManager) {
        super(imageManager);
    }

    @Override
    protected Object doFetchImage(String path, int width, int height, final ImageReadyCallback cb) {
        return imageManager.getImage(path, new LoadedCallback() {
            @Override
            public void onLoadCompleted(Bitmap loaded) {
                cb.onImageReady(loaded);
            }

            @Override
            public void onLoadFailed(Exception e) {
                cb.onException(e);
            }
        });
    }
}
