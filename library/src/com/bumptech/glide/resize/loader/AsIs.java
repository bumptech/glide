package com.bumptech.glide.resize.loader;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.ViewGroup;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.LoadedCallback;

/**
 * Load an image at its original dimensions.
 *
 * <p> Should be used when an image is exactly the same size as the view that will display it
 * or you want to use some external process (like the view) to do the resizing for you. This class is usually less
 * efficient than other implementations if the image is not exactly the size of the view
 * </p>
 *
 * @see ImageManager#getImage(String, com.bumptech.glide.loader.stream.StreamLoader, com.bumptech.glide.resize.LoadedCallback)
 */
@SuppressWarnings("unused")
public class AsIs extends ImageManagerLoader {

    public AsIs(Context context) {
        super(context);
    }

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

    @Override
    protected boolean isHandled(int width, int height) {
        return super.isHandled(width, height)
                || (width == ViewGroup.LayoutParams.WRAP_CONTENT || height == ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
