/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.resize.loader;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.LoadedCallback;

/**
 * Load an image at its original dimensions.
 *
 * <p>
 *     This class is almost identical to {@link AsIs} except that it assumes the image size is identical to the given
 *     dimensions. This assumption means we don't first need to read the image header to obtain the dimensions.
 * </p>
 *
 * @see ImageManager#getImageExact(String, com.bumptech.glide.loader.stream.StreamLoader, int, int, com.bumptech.glide.resize.LoadedCallback)
 */
@SuppressWarnings("unused")
public class Exact extends ImageManagerLoader {

    public Exact(Context context) {
        super(context);
    }

    public Exact(ImageManager imageManager) {
        super(imageManager);
    }

    @Override
    protected Object loadFromImageManager(String id, StreamLoader streamLoader, int width, int height, final ImageReadyCallback cb) {
        return imageManager.getImageExact(id, streamLoader, width, height, new LoadedCallback() {
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
