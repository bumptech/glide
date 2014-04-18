/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.presenter;

import android.content.Context;
import com.bumptech.glide.presenter.target.Target;

/**
 *Wraps a pair of {@link ImagePresenter} objects and uses them to load and display an image and a thumbnail for that
 * image. The image load is started first. If the image load does not return synchronously (ie if the image is not
 * cached), then the thumbnail load is started. If the thumbnail load does not return synchronously, the placeholder
 * image is shown. If the thumbnail load completes before the image load (expected in most cases), the thumbnail is
 * shown until the image load completes. If the image load completes first, the thumbnail will never be shown.
 */
@SuppressWarnings("unused")
public class ThumbImagePresenter<A, B extends Target> implements ImagePresenterCoordinator,
        Presenter<A> {

    /**
     * A builder for a {@link ThumbImagePresenter}. Has a few convenience methods to avoid identical calls on both
     * the full and thumb presenter. {@link Builder ThumbImagePresenter.Builder#setFullPresenterBuilder},
     * {@link Builder ThumbImagePresenter.Builder#setThumbPresenterBuilder}, and
     * {@link Builder ThumbImagePresenter.Builder#setImageView} are required.
     *
     * @param <A> The type of the model that the full {@link ImagePresenter} requires
     * @param <B> The type of the model that the thumb {@link ImagePresenter} requires
     */
    public static class Builder<A, B extends Target> {
        private ImagePresenter.Builder<A, B> fullPresenterBuilder;
        private ImagePresenter.Builder<A, B> thumbPresenterBuilder;
        private Context context;
        private B target;

        public ThumbImagePresenter<A, B> build(){
            if (fullPresenterBuilder == null) {
                throw new IllegalArgumentException("you must include a builder for the full image presenter");
            }
            if (thumbPresenterBuilder == null) {
                throw new IllegalArgumentException("you must include a builder for the thumb image presenter");
            }
            if (target == null) {
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
        public Builder<A, B> setFullPresenterBuilder(ImagePresenter.Builder<A, B> builder) {
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
        public Builder<A, B> setThumbPresenterBuilder(ImagePresenter.Builder<A, B> builder) {
            this.thumbPresenterBuilder = builder;
            return this;
        }

        /**
         * @see ImagePresenter.Builder ImagePresenter.Builder#setImageView
         */
        public Builder<A, B> setTarget(B target, Context context) {
            this.target = target;
            this.context = context;
            return this;
        }
    }

    private final ImagePresenter<A, B> fullPresenter;
    private final ImagePresenter<A, B> thumbPresenter;

    private ThumbImagePresenter(Builder<A, B> builder) {
        fullPresenter = builder.fullPresenterBuilder
                .setImagePresenterCoordinator(this)
                .setTarget(builder.target, builder.context)
                .build();

        thumbPresenter = builder.thumbPresenterBuilder
                .setImagePresenterCoordinator(this)
                .setTarget(builder.target, builder.context)
                .build();

        builder.target.setPresenter(this);
    }

    /**
     * Sets models to load the full and thumb image. If the load for fullModel finishes before the one for thumbModel
     * then only the full image will be shown
     *
     * @see ImagePresenter#setModel(Object)
     */
    public void setModel(A model) {
        fullPresenter.setModel(model);
        if (!fullPresenter.isImageSet()) {
            thumbPresenter.setModel(model);
        }
    }

    @Override
    public void resetPlaceHolder() {
        fullPresenter.resetPlaceHolder();
    }

    /**
     * Calls the corresponding method on both image presenters
     *
     * @see ImagePresenter#clear()
     */
    @Override
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

    @Override
    public boolean canCallReadyCallback(ImagePresenter presenter) {
        return presenter == thumbPresenter || !thumbPresenter.isImageSet();
    }

    @Override
    public boolean canCallErrorCallback(ImagePresenter presenter) {
        return presenter == fullPresenter;
    }
}
