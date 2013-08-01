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
 * Load the image so that one dimension of the image exactly matches one of the given dimensions and the other dimension
 * of the image is smaller than or equal to the other given dimension.
 *
 * @see ImageManager#fitCenter(String, com.bumptech.glide.loader.stream.StreamLoader, int, int, com.bumptech.glide.resize.LoadedCallback)
 */
@SuppressWarnings("Unused")
public class FitCenter extends ImageManagerLoader {

    public FitCenter(Context context) {
        super(context, Transformation.FIT_CENTER);
    }

    public FitCenter(ImageManager imageManager) {
        super(imageManager, Transformation.FIT_CENTER);
    }
}
