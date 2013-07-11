package com.bumptech.photos.loader.model;

import com.bumptech.photos.loader.opener.StreamOpener;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/11/13
 * Time: 11:54 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class DirectModelStreamLoader<T> extends BaseModelStreamLoader<T> {
    @Override
    protected void doFetchModelStreams(T model, int width, int height, ModelStreamsReadyCallback cb) {
        cb.onStreamsReady(getId(model), getStreamOpener(model, width, height));
    }

    @Override
    protected boolean onModelStreamFetchFailed(Exception e, T model) {
        return false;
    }

    protected abstract StreamOpener getStreamOpener(T model, int width, int height);
    protected abstract String getId(T model);
}
