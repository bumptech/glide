/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.resize.loader;

import android.graphics.Bitmap;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.LoadedCallback;

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

    public Approximate(ImageManager imageManager) {
        super(imageManager);
    }

    @Override
    protected Object loadFromImageManager(String id, StreamLoader streamLoader, int width, int height, final ImageReadyCallback cb) {
        return imageManager.getImageApproximate(id, streamLoader, width, height, new LoadedCallback() {
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
