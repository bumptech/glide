/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.resize.loader;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.LoadedCallback;
import com.bumptech.glide.resize.Transformation;

/**
 * Load image to exactly match the view in one dimension and then crop the image to fit the other dimension.
 *
 * @see ImageManager#centerCrop(String, com.bumptech.glide.loader.stream.StreamLoader, int, int, com.bumptech.glide.resize.LoadedCallback)
 */
@SuppressWarnings("unused")
public class CenterCrop extends ImageManagerLoader {

    public CenterCrop(Context context) {
        super(context, Transformation.CENTER_CROP);
    }

    public CenterCrop(ImageManager imageManager) {
        super(imageManager, Transformation.CENTER_CROP);
    }
}
