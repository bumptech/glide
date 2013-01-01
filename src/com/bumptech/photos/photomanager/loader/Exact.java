/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.photomanager.loader;

import com.bumptech.photos.photomanager.LoadedCallback;
import com.bumptech.photos.photomanager.PhotoManager;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/25/12
 * Time: 10:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class Exact<T> extends PhotoManagerLoader<T> {

    public Exact(PhotoManager photoManager) {
        super(photoManager);
    }

    @Override
    protected Object doFetchImage(String path, int width, int height, LoadedCallback cb) {
        return photoManager.getImageExact(path, width, height, cb);
    }
}
