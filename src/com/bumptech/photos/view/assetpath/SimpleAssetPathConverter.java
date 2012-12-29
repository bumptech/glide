/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.view.assetpath;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/25/12
 * Time: 8:52 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class SimpleAssetPathConverter implements AssetPathConverter<String> {
    @Override
    public void fetchPath(String path, PathReadyListener listener) {
        listener.onPathReady(path);
    }
}
