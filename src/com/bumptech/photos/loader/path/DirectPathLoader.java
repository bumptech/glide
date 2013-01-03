/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.loader.path;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/25/12
 * Time: 8:52 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class DirectPathLoader<T> implements PathLoader<T> {

    @Override
    public final Object fetchPath(T model, int width, int height, PathReadyCallback cb) {
        cb.onPathReady(getPath(model, width, height));
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected abstract String getPath(T model, int width, int height);

    @Override
    public final void clear() { }
}
