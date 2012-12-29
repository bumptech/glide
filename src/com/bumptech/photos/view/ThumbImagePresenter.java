/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.view;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import com.bumptech.photos.view.assetpath.AssetPathConverter;
import com.bumptech.photos.view.loader.ImageLoader;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/28/12
 * Time: 12:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThumbImagePresenter<T> implements ImagePresenter.AssetPresenterCoordinator<T> {
    private final ImagePresenter<T> fullPresenter;
    private final ImagePresenter<T> thumbPresenter;

    public ThumbImagePresenter(ImageView imageView, AssetPathConverter<T> converter, ImageLoader fullLoader, ImageLoader thumbLoader) {
        this(new ImagePresenter<T>(imageView, converter,  fullLoader), new ImagePresenter<T>(imageView, converter, thumbLoader));
    }

    public ThumbImagePresenter(ImagePresenter<T> full, ImagePresenter<T> thumb) {
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

    public void setOnFullSetCallback(ImageSetCallback cb) {
        fullPresenter.setOnImageSetCallback(cb);
    }

    public void setOnThumbSetCallback(ImageSetCallback cb) {
        thumbPresenter.setOnImageSetCallback(cb);
    }

    public void setPlaceholderDrawable(Drawable placeholderDrawable) {
        fullPresenter.setPlaceholderDrawable(placeholderDrawable);
    }

    public void clear(){
        fullPresenter.clear();
        thumbPresenter.clear();
    }

    @Override
    public boolean canSetImage(ImagePresenter presenter) {
        return presenter == fullPresenter || !fullPresenter.isImageSet();
    }

    @Override
    public boolean canSetPlaceholder(ImagePresenter presenter) {
        return presenter == fullPresenter;
    }
}
