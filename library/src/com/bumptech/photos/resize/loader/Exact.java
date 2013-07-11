/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.resize.loader;

import android.graphics.Bitmap;
import com.bumptech.photos.loader.opener.StreamOpener;
import com.bumptech.photos.resize.ImageManager;
import com.bumptech.photos.resize.LoadedCallback;

/**
 * An ImageLoader implementation that loads an image at the given path and expects that the image at that path
 * will exactly match the width and height of the view that will display it. Less expensive than other implementations,
 * but requires some other process to make sure the image on disk matches the given dimension (for example a server side
 * resize).
 *
 * @see ImageManager#getImageExact(String, com.bumptech.photos.loader.opener.StreamOpener, int, int, com.bumptech.photos.resize.LoadedCallback)
 */
public class Exact extends ImageManagerLoader {

    public Exact(ImageManager imageManager) {
        super(imageManager);
    }

    @Override
    protected Object loadFromImageManager(String id, StreamOpener streamOpener, int width, int height, final ImageReadyCallback cb) {
        return imageManager.getImageExact(id, streamOpener, width, height, new LoadedCallback() {
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
