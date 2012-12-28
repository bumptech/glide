/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.view.loader;

import android.graphics.Bitmap;
import com.bumptech.photos.LoadedCallback;
import com.bumptech.photos.PhotoManager;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/28/12
 * Time: 9:53 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ImageLoader {
    protected final PhotoManager photoManager;
    private Bitmap acquired = null;
    private Bitmap ready = null;
    private Object loadToken = null;

    public interface ImageReadyCallback {
        public void onImageReady();
        public void onLoadFailed(Exception e);
    }

    public ImageLoader(PhotoManager photoManager) {
        this.photoManager = photoManager;
    }

    public void loadImage(String path, int width, int height, final ImageReadyCallback callback) {
        photoManager.cancelTask(loadToken);

        loadToken = doLoad(path, width, height, new LoadedCallback() {
            @Override
            public void onLoadCompleted(Bitmap loaded) {
                ready = loaded;
                callback.onImageReady();
            }

            @Override
            public void onLoadFailed(Exception e) {
                callback.onLoadFailed(e);
            }
        });
    }

    public Bitmap getReadyBitmap() {
        if (acquired != null) {
            photoManager.releaseBitmap(acquired);
            acquired = null;
        }
        if (ready != null) {
            photoManager.acquireBitmap(ready);
            acquired = ready;
            ready = null;
        }
        return acquired;
    }

    protected abstract Object doLoad(String path, int width, int height, LoadedCallback cb);
}
