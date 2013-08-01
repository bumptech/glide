package com.bumptech.glide.resize.loader;

import android.content.Context;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.Transformation;

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
        super(context, Transformation.CENTER_CROP);
    }

    public AsIs(ImageManager imageManager) {
        super(imageManager, Transformation.CENTER_CROP);
    }
}
