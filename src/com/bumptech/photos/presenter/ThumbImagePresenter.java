/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.presenter;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/28/12
 * Time: 12:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThumbImagePresenter<T> implements ImagePresenter.AssetPresenterCoordinator<T> {

    public static class Builder<T> {
        private ImagePresenter.Builder<T> fullPresenterBuilder;
        private ImagePresenter.Builder<T> thumbPresenterBuilder;
        private ImageView imageView;
        private Drawable placeholderDrawable;
        private int placeholderResourceId;

        public ThumbImagePresenter<T> build(){
            assert fullPresenterBuilder != null : "you must include a builder for the full image presenter";
            assert thumbPresenterBuilder != null : "you must include a builder for the thumb image presenter";
            assert imageView != null : "cannot create presenter without an image view";

            return new ThumbImagePresenter<T>(this);
        }

        public Builder<T> setFullPresenterBuilder(ImagePresenter.Builder<T> builder) {
            this.fullPresenterBuilder = builder;
            return this;
        }

        public Builder<T> setThumbPresenterBuilder(ImagePresenter.Builder<T> builder) {
            this.thumbPresenterBuilder = builder;
            return this;
        }

        public Builder<T> setImageView(ImageView imageView) {
            this.imageView = imageView;
            return this;
        }

        public Builder<T> setPlaceholderDrawable(Drawable drawable) {
            assert drawable == null || placeholderResourceId == 0 : "Can't set both a placeholder drawable and a placeholder resource";

            this.placeholderDrawable = drawable;
            return this;
        }

        public Builder<T> setPlaceholderResource(int resourceId) {
            assert resourceId == 0 || placeholderDrawable == null : "Can't set both a placeholder drawable and a placeholder resource";

            this.placeholderResourceId = resourceId;
            return this;
        }
    }

    private final ImagePresenter<T> fullPresenter;
    private final ImagePresenter<T> thumbPresenter;

    private ThumbImagePresenter(Builder<T> builder) {
        fullPresenter = builder.fullPresenterBuilder
                .setAssetPresenterCoordinator(this)
                .setImageView(builder.imageView)
                .setPlaceholderResource(0)
                .setPlaceholderDrawable(builder.placeholderDrawable == null ?
                        builder.imageView.getResources().getDrawable(builder.placeholderResourceId) : builder.placeholderDrawable)
                .build();
        thumbPresenter = builder.thumbPresenterBuilder
                .setAssetPresenterCoordinator(this)
                .setImageView(builder.imageView)
                .setPlaceholderDrawable(null)
                .setPlaceholderResource(0)
                .build();
    }

    public void setAssetModels(T fullModel, T thumbModel) {
        fullPresenter.setAssetModel(fullModel);
        if (!fullPresenter.isImageSet()) {
            thumbPresenter.setAssetModel(thumbModel);
        }
    }

    public ImageView getImageView() {
        return fullPresenter.getImageView();
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

    public void resetPlaceholder() {
        fullPresenter.resetPlaceHolder();
    }
}
