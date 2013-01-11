/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.resize.loader;

import android.graphics.Bitmap;
import com.bumptech.photos.resize.ImageManager;
import com.bumptech.photos.resize.LoadedCallback;

import java.util.concurrent.Future;

/**
 * An ImageLoader implementation that loads an image into within the given dimensions maintaining the original
 * proportions
 *
 * @see ImageManager#fitCenter(String, int, int, com.bumptech.photos.resize.LoadedCallback)
 */
public class FitCenter<T> extends ImageManagerLoader<T> {

    public FitCenter(ImageManager imageManager) {
        super(imageManager);
    }

    @Override
    protected Future doFetchImage(String path, int width, int height, final ImageReadyCallback cb) {
        return imageManager.fitCenter(path, width, height, new LoadedCallback() {
            @Override
            public void onLoadCompleted(Bitmap loaded) {
                cb.onImageReady(loaded);
            }

            @Override
            public void onLoadFailed(Exception e) {
                cb.onError(e);
            }
        });
    }
}
