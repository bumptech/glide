package com.bumptech.glide.resize.loader;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.Glide;
import com.bumptech.glide.loader.image.BaseImageLoader;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.resize.Downsampler;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.LoadedCallback;
import com.bumptech.glide.resize.Transformation;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * A base class for loaders that user ImageManager. Primarily responsible for keeping track of bitmaps for recycling
 * purposes.
 */
public class ImageManagerLoader extends BaseImageLoader {

    protected final ImageManager imageManager;
    private final Transformation transformation;
    private final Downsampler downsampler;
    private Bitmap acquired;
    private Object loadToken;

    public ImageManagerLoader(Context context, Transformation transformation) {
        this(context, Downsampler.AT_LEAST, transformation);
    }

    public ImageManagerLoader(Context context, Downsampler downsampler, Transformation transformation) {
        this(Glide.get().getImageManager(context), downsampler, transformation);
    }

    public ImageManagerLoader(ImageManager imageManager, Transformation transformation) {
        this(imageManager, Downsampler.AT_LEAST, transformation);
    }

    public ImageManagerLoader(ImageManager imageManager, Downsampler downsampler, Transformation transformation) {
        this.imageManager = imageManager;
        this.downsampler = downsampler;
        this.transformation = transformation;
    }

    @Override
    protected final void doFetchImage(String id, StreamLoader streamLoader, int width, int height, ImageReadyCallback cb) {
        if (!isHandled(width, height)) {
            throw new IllegalArgumentException(getClass() + " cannot handle width=" + width + " and/or height =" +
                    height);
        }
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
    protected Object loadFromImageManager(String id, StreamLoader streamLoader, int width, int height, final ImageReadyCallback cb) {
        return imageManager.getImage(id, streamLoader, width, height, downsampler, transformation, new LoadedCallback() {
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

    protected boolean isHandled(int width, int height) {
        return width >= 0 && height >= 0 ||
                (downsampler == Downsampler.NONE && width == WRAP_CONTENT && height == WRAP_CONTENT);
    }
}
