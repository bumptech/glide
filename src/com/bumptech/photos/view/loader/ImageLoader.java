/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.view.loader;

import com.bumptech.photos.LoadedCallback;
import com.bumptech.photos.PhotoManager;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/25/12
 * Time: 10:49 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ImageLoader {
    public Object loadImage(PhotoManager photoManager, String path, int width, int height, LoadedCallback cb);
    public void onLoadFailed(Exception e);
}
