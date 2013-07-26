/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.resize.loader;

import android.graphics.Bitmap;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.LoadedCallback;

/**
 * Load the image so that one dimension of the image exactly matches one of the given dimensions and the other dimension
 * of the image is smaller than or equal to the other given dimension.
 *
 * @see ImageManager#fitCenter(String, com.bumptech.glide.loader.stream.StreamLoader, int, int, com.bumptech.glide.resize.LoadedCallback)
 */
@SuppressWarnings("Unused")
public class FitCenter extends ImageManagerLoader {

    public FitCenter(ImageManager imageManager) {
        super(imageManager);
    }

    @Override
    protected Object loadFromImageManager(String id, StreamLoader streamLoader, int width, int height, final ImageReadyCallback cb) {
        return imageManager.fitCenter(id, streamLoader, width, height, new LoadedCallback() {
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
