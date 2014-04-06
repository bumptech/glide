package com.bumptech.glide.loader.image;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.Glide;
import com.bumptech.glide.resize.BitmapLoad;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.LoadedCallback;

/**
 * A base class for loaders that user ImageManager. Primarily responsible for keeping track of bitmaps for recycling
 * purposes.
 */
public class ImageManagerLoader implements ImageLoader {
    private final ImageManager imageManager;
    private Bitmap acquired;
    private ImageManager.LoadToken loadToken;

    public ImageManagerLoader(Context context) {
        this(Glide.get(context).getImageManager());
    }

    public ImageManagerLoader(ImageManager imageManager) {
        this.imageManager = imageManager;
    }

    @Override
    public Object fetchImage(BitmapLoad loadTask, final ImageReadyCallback cb) {
        loadToken = imageManager.getImage(loadTask, new LoadedCallback() {
            @Override
            public void onLoadCompleted(Bitmap loaded) {
                onImageReady(loaded, cb.onImageReady(loaded));
            }

            @Override
            public void onLoadFailed(Exception e) {
                cb.onException(e);
            }
        });
        return loadToken;
    }

    private void onImageReady(Bitmap image, boolean isUsed) {
        if (isUsed) {
            releaseAcquired();
            acquired = image;
        } else {
            imageManager.releaseBitmap(image);
        }
    }

    @Override
    public void clear() {
        releaseAcquired();
        if (loadToken != null) {
            loadToken.cancel();
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
