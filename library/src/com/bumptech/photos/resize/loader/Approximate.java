/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.resize.loader;

import android.graphics.Bitmap;
import com.bumptech.photos.resize.ImageManager;
import com.bumptech.photos.resize.LoadedCallback;

/**
 * An ImageLoader implementation that loads an image to roughly the width and height of the view that will display it.
 * Should be used when the image is larger than the view that will display it but the expense of cropping or resizing
 * the image more precisely is not worth it. Can save a substantial amount of memory depending on the size discrepancy
 *
 * @see ImageManager#getImageApproximate(String, int, int, com.bumptech.photos.resize.LoadedCallback)
 */
public class Approximate<T> extends ImageManagerLoader<T> {

    public Approximate(ImageManager imageManager) {
        super(imageManager);
    }

    @Override
    protected Object doFetchImage(String path, int width, int height, final ImageReadyCallback cb) {
        return imageManager.getImageApproximate(path, width, height, new LoadedCallback() {
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
