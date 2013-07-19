package com.bumptech.glide.resize.loader;

import android.graphics.Bitmap;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.LoadedCallback;

/**
 * An ImageLoader implementation that loads an image at the given path at its original dimensions. Should be used
 * when an image is roughly the same size as the view that will display it and you want to use some external process
 * (like the view) to do the resizing for you. Not memory efficient and more expensive to use recycled Bitmaps for than
 * other implementations
 *
 * @see ImageManager#getImage(String, com.bumptech.glide.loader.stream.StreamLoader, com.bumptech.glide.resize.LoadedCallback)
 */
public class AsIs extends ImageManagerLoader {

    public AsIs(ImageManager imageManager) {
        super(imageManager);
    }

    @Override
    protected Object loadFromImageManager(String id, StreamLoader streamLoader, int width, int height, final ImageReadyCallback cb) {
        return imageManager.getImage(id, streamLoader, new LoadedCallback() {
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
