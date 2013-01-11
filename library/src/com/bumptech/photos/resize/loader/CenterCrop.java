/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.resize.loader;

import android.graphics.Bitmap;
import com.bumptech.photos.resize.ImageManager;
import com.bumptech.photos.resize.LoadedCallback;

/**
 * An ImageLoader implementation that loads and crops in image down to the given width and height.
 *
 * @see ImageManager#centerCrop(String, int, int, com.bumptech.photos.resize.LoadedCallback)
 */
public class CenterCrop<T> extends ImageManagerLoader<T> {

    public CenterCrop(ImageManager imageManager) {
        super(imageManager);
    }

    @Override
    protected Object doFetchImage(String path, int width, int height, final ImageReadyCallback cb) {
        return imageManager.centerCrop(path, width, height, new LoadedCallback() {
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
