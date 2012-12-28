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
 * Time: 10:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class Approximate extends ImageLoader {

    public Approximate(PhotoManager photoManager) {
        super(photoManager);
    }

    @Override
    protected Object doLoad(String path, int width, int height, LoadedCallback cb) {
        return photoManager.getImage(path, width, height, cb);
    }
}
