/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.resize.loader;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.resize.Downsampler;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.LoadedCallback;
import com.bumptech.glide.resize.Transformation;

/**
 * Load an image at roughly the width and height of the view that will display it, maintaining its original aspect ratio
 *
 * <p>
 * Should be used when the image is larger than the view that will display it and you don't want to alter the original
 * aspect ratio. Can save a substantial amount of memory depending on the size discrepancy between the view and the
 * image.
 * </p>
 *
 * @see ImageManager#getImageApproximate(String, com.bumptech.glide.loader.stream.StreamLoader, int, int, com.bumptech.glide.resize.LoadedCallback)
 */
@SuppressWarnings("unused")
public class Approximate extends ImageManagerLoader {

    public Approximate(Context context) {
        super(context, Downsampler.AT_LEAST, Transformation.NONE);
    }

    public Approximate(ImageManager imageManager) {
        super(imageManager, Downsampler.AT_LEAST, Transformation.NONE);
    }
}
