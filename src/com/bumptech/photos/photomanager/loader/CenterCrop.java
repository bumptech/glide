/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.photomanager.loader;

import com.bumptech.photos.photomanager.LoadedCallback;
import com.bumptech.photos.photomanager.PhotoManager;
import com.bumptech.photos.photomanager.PhotoManagerLoader;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/25/12
 * Time: 10:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class CenterCrop<T> extends PhotoManagerLoader<T> {

    public CenterCrop(PhotoManager photoManager) {
        super(photoManager);
    }

    @Override
    protected Object doFetchImage(String path, int width, int height, LoadedCallback cb) {
        return photoManager.centerCrop(path, width, height, cb);
    }
}
