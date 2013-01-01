/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.view.loader;

import android.graphics.Bitmap;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/29/12
 * Time: 11:32 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ImageLoader<T>  {
    public interface PathReadyCallback {
        public boolean onPathReady(String path);
        public void onError(Exception e);
    }

    public interface ImageReadyCallback {
        public boolean onImageReady(Bitmap image);
        public void onError(Exception e);
    }

    public Object fetchPath(T model, int width, int height, PathReadyCallback cb);
    public Object fetchImage(String path, T model, int width, int height, ImageReadyCallback cb);
    public void clear();

}
