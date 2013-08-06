/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.presenter;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 *Wraps a pair of {@link ImagePresenter} objects and uses them to load and display an image and a thumbnail for that
 * image. The image load is started first. If the image load does not return synchronously (ie if the image is not
 * cached), then the thumbnail load is started. If the thumbnail load does not return synchronously, the placeholder
 * image is shown. If the thumbnail load completes before the image load (expected in most cases), the thumbnail is
 * shown until the image load completes. If the image load completes first, the thumbnail will never be shown.
 */
@SuppressWarnings("unused")
public class ThumbImagePresenter<A, B> implements ImagePresenter.ImagePresenterCoordinator {

    /**
     * A builder for a {@link ThumbImagePresenter}. Has a few convenience methods to avoid identical calls on both
     * the full and thumb presenter. {@link Builder ThumbImagePresenter.Builder#setFullPresenterBuilder},
     * {@link Builder ThumbImagePresenter.Builder#setThumbPresenterBuilder}, and
     * {@link Builder ThumbImagePresenter.Builder#setImageView} are required.
     *
     * @param <A> The type of the model that the full {@link ImagePresenter} requires
     * @param <B> The type of the model that the thumb {@link ImagePresenter} requires
     */
    public static class Builder<A, B> {
        private ImagePresenter.Builder<A> fullPresenterBuilder;
        private ImagePresenter.Builder<B> thumbPresenterBuilder;
        private ImageView imageView;
        private Drawable placeholderDrawable;
        private int placeholderResourceId;

        public ThumbImagePresenter<A, B> build(){
            if (fullPresenterBuilder == null) {
                throw new IllegalArgumentException("you must include a builder for the full image presenter");
            }
            if (thumbPresenterBuilder == null) {
                throw new IllegalArgumentException("you must include a builder for the thumb image presenter");
            }
            if (imageView == null){
                throw new IllegalArgumentException("cannot create presenter without an image view");
            }

            return new ThumbImagePresenter<A, B>(this);
        }

        /**
         * Required - sets the {@link ImagePresenter} that will be used to load the full image
         *
         * @param builder A builder that will produce an ImagePresenter. At least {@link ImagePresenter.Builder ImagePresenter.Builder#setPathLoader},
         *                and {@link ImagePresenter.Builder ImagePresenter.Builder#setImageLoader} must have been called
         * @return This builder object
         */
        public Builder<A, B> setFullPresenterBuilder(ImagePresenter.Builder<A> builder) {
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
        public Builder<A, B> setThumbPresenterBuilder(ImagePresenter.Builder<B> builder) {
            this.thumbPresenterBuilder = builder;
            return this;
        }

        /**
         * @see ImagePresenter.Builder ImagePresenter.Builder#setImageView
         */
        public Builder<A, B> setImageView(ImageView imageView) {
            this.imageView = imageView;
            return this;
        }

        /**
         * @see ImagePresenter.Builder ImagePresenter.Builder#setPlaceholderDrawable
         */
        public Builder<A, B> setPlaceholderDrawable(Drawable drawable) {
            if (drawable != null && this.placeholderResourceId != 0) {
                throw new IllegalArgumentException("Can't set both a placeholder drawable and a placeholder resource");
            }

            this.placeholderDrawable = drawable;
            return this;
        }

        /**
         * @see ImagePresenter.Builder ImagePresenter.Builder#setPlaceholderResource
         */
        public Builder<A, B> setPlaceholderResource(int resourceId) {
            if (resourceId != 0 && placeholderDrawable != null) {
                throw new IllegalArgumentException("Can't set both a placeholder drawable and a placeholder resource");
            }

            this.placeholderResourceId = resourceId;
            return this;
        }
    }

    private final ImagePresenter<A> fullPresenter;
    private final ImagePresenter<B> thumbPresenter;

    private ThumbImagePresenter(Builder<A, B> builder) {
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
    public void setModels(A fullModel, B thumbModel) {
        fullPresenter.setModel(fullModel);
        if (!fullPresenter.isImageSet()) {
            thumbPresenter.setModel(thumbModel);
        }
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
