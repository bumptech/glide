/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.view.assetpath;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/25/12
 * Time: 8:51 PM
 * To change this template use File | Settings | File Templates.
 */
public interface AssetPathConverter<T> {
    public interface PathReadyListener {
        public void pathReady(String path);
        public void onError(Exception e);
    }

    public void fetchPath(T model, int width, int height, PathReadyListener listener);
}
