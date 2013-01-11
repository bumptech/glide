/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.resize.loader;

import android.graphics.Bitmap;
import com.bumptech.photos.resize.ImageManager;
import com.bumptech.photos.resize.LoadedCallback;

/**
 * An ImageLoader implementation that loads an image at the given path and expects that the image at that path
 * will exactly match the width and height of the view that will display it. Less expensive than other implementations,
 * but requires some other process to make sure the image on disk matches the given dimension (for example a server side
 * resize).
 *
 * @see ImageManager#getImageExact(String, int, int, com.bumptech.photos.resize.LoadedCallback)
 */
public class Exact<T> extends ImageManagerLoader<T> {

    public Exact(ImageManager imageManager) {
        super(imageManager);
    }

    @Override
    protected Object doFetchImage(String path, int width, int height, final ImageReadyCallback cb) {
        return imageManager.getImageExact(path, width, height, new LoadedCallback() {
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
