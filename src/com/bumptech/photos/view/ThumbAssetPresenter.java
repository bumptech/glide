/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.view;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/28/12
 * Time: 12:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThumbAssetPresenter<T> implements AssetPresenter.AssetPresenterCoordinator<T> {
    private final AssetPresenter<T> fullPresenter;
    private final AssetPresenter<T> thumbPresenter;

    public ThumbAssetPresenter(AssetPresenter<T> full, AssetPresenter<T> thumb) {
        fullPresenter = full;
        thumbPresenter = thumb;
        thumbPresenter.setCoordinator(this);
        fullPresenter.setCoordinator(this);
    }

    public void setAssetModels(T fullModel, T thumbModel) {
        fullPresenter.setAssetModel(fullModel);
        if (!fullPresenter.isImageSet()) {
            thumbPresenter.setAssetModel(thumbModel);
        }
    }

    public void setDimens(int width, int height) {
        fullPresenter.setDimens(width, height);
        thumbPresenter.setDimens(width, height);
    }

    public void clear(){
        fullPresenter.clear();
        thumbPresenter.clear();
    }

    @Override
    public boolean canSetImage(AssetPresenter presenter) {
        return presenter == fullPresenter || !fullPresenter.isImageSet();
    }

    @Override
    public boolean canSetPlaceholder(AssetPresenter presenter) {
        return presenter == fullPresenter;
    }
}
