/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.presenter;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 *Wraps a pair of {@link ImagePresenter} objects and uses them to load and display an image and a thumbnail for that
 * image. The image load is started first. If the image load does not return synchronously (ie if the image is not
 * cached), then the thumbnail load is started. If the thumbnail load does not return synchronously, the placeholder
 * image is shown. If the thumbnail load completes before the image load (expected in most cases), the thumbnail is
 * shown until the image load completes. If the image load completes first, the thumbnail will never be shown.
 *
 */
public class ThumbImagePresenter<T> implements ImagePresenter.ImagePresenterCoordinator<T> {

    /**
     * A builder for a {@link ThumbImagePresenter}. Has a few convenience methods to avoid identical calls on both
     * the full and thumb presenter. {@link Builder ThumbImagePresenter.Builder#setFullPresenterBuilder},
     * {@link Builder ThumbImagePresenter.Builder#setThumbPresenterBuilder}, and
     * {@link Builder ThumbImagePresenter.Builder#setImageView} are required.
     *
     * @param <T> The type of the model that the full and thumb presenters require to load a path and an image for that
     *           path
     */
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

        /**
         * Required - sets the {@link ImagePresenter} that will be used to load the full image
         *
         * @param builder A builder that will produce an ImagePresenter. At least {@link ImagePresenter.Builder ImagePresenter.Builder#setPathLoader},
         *                and {@link ImagePresenter.Builder ImagePresenter.Builder#setImageLoader} must have been called
         * @return This builder object
         */
        public Builder<T> setFullPresenterBuilder(ImagePresenter.Builder<T> builder) {
            this.fullPresenterBuilder = builder;
            return this;
        }

        /**
         * Required - sets the {@link ImagePresenter} that will be used to load the thumbnail
         *
         * @param builder A builder that will produce an ImagePresenter. At least {@link ImagePresenter.Builder ImagePresenter.Builder#setPathLoader},
         *                and {@link ImagePresenter.Builder ImagePresenter.Builder#setImageLoader} must have been called
         * @return This builder object
         */
        public Builder<T> setThumbPresenterBuilder(ImagePresenter.Builder<T> builder) {
            this.thumbPresenterBuilder = builder;
            return this;
        }

        /**
         * @see ImagePresenter.Builder ImagePresenter.Builder#setImageView
         */
        public Builder<T> setImageView(ImageView imageView) {
            this.imageView = imageView;
            return this;
        }

        /**
         * @see ImagePresenter.Builder ImagePresenter.Builder#setPlaceholderDrawable
         */
        public Builder<T> setPlaceholderDrawable(Drawable drawable) {
            assert drawable == null || placeholderResourceId == 0 : "Can't set both a placeholder drawable and a placeholder resource";

            this.placeholderDrawable = drawable;
            return this;
        }

        /**
         * @see ImagePresenter.Builder ImagePresenter.Builder#setPlaceholderResource
         */
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
                .setImagePresenterCoordinator(this)
                .setImageView(builder.imageView)
                .setPlaceholderResource(0)
                .setPlaceholderDrawable(builder.placeholderDrawable == null ?
                        builder.imageView.getResources().getDrawable(builder.placeholderResourceId) : builder.placeholderDrawable)
                .build();
        thumbPresenter = builder.thumbPresenterBuilder
                .setImagePresenterCoordinator(this)
                .setImageView(builder.imageView)
                .setPlaceholderDrawable(null)
                .setPlaceholderResource(0)
                .build();
    }

    /**
     * Sets models to load the full and thumb image. If the load for fullModel finishes before the one for thumbModel
     * then only the full image will be shown
     *
     * @see ImagePresenter#setModel(Object)
     */
    public void setModels(T fullModel, T thumbModel) {
        fullPresenter.setModel(fullModel);
        if (!fullPresenter.isImageSet()) {
            thumbPresenter.setModel(thumbModel);
        }
    }

    /**
     * Get the {@link android.widget.ImageView} this object is acting on
     *
     * @see ImagePresenter#getImageView()
     */
    public ImageView getImageView() {
        return fullPresenter.getImageView();
    }

    public void setDimens(int width, int height) {
        fullPresenter.setDimens(width, height);
        thumbPresenter.setDimens(width, height);
    }

    /**
     * Calls the corresponding method on both image presenters
     *
     * @see ImagePresenter#clear()
     */
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

    /**
     * Show the placeholder if one is set, otherwise clear the view
     *
     * @see ImagePresenter#resetPlaceHolder()
     */
    public void resetPlaceholder() {
        fullPresenter.resetPlaceHolder();
    }
}
