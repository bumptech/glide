/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.resize.loader;

import android.graphics.Bitmap;
import com.bumptech.glide.loader.opener.StreamOpener;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.LoadedCallback;

/**
 * An ImageLoader implementation that loads and crops in image down to the given width and height.
 *
 * @see ImageManager#centerCrop(String, com.bumptech.glide.loader.opener.StreamOpener, int, int, com.bumptech.glide.resize.LoadedCallback)
 */
public class CenterCrop extends ImageManagerLoader {

    public CenterCrop(ImageManager imageManager) {
        super(imageManager);
    }

    @Override
    protected Object loadFromImageManager(String id, StreamOpener streamOpener, int width, int height, final ImageReadyCallback cb) {
        return imageManager.centerCrop(id, streamOpener, width, height, new LoadedCallback() {
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
