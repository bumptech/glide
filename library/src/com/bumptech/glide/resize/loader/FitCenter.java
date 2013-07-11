/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.resize.loader;

import android.graphics.Bitmap;
import com.bumptech.glide.loader.opener.StreamOpener;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.LoadedCallback;

/**
 * An ImageLoader implementation that loads an image into within the given dimensions maintaining the original
 * proportions
 *
 * @see ImageManager#fitCenter(String, com.bumptech.glide.loader.opener.StreamOpener, int, int, com.bumptech.glide.resize.LoadedCallback)
 */
public class FitCenter extends ImageManagerLoader {

    public FitCenter(ImageManager imageManager) {
        super(imageManager);
    }

    @Override
    protected Object loadFromImageManager(String id, StreamOpener streamOpener, int width, int height, final ImageReadyCallback cb) {
        return imageManager.fitCenter(id, streamOpener, width, height, new LoadedCallback() {
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
