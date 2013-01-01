/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.view;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import com.bumptech.photos.util.Log;
import com.bumptech.photos.view.loader.ImageLoader;

import java.lang.ref.WeakReference;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/25/12
 * Time: 10:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImagePresenter<T> {
    private Object pathToken;
    private Object imageToken;

    public static class Builder<T> {
        private ImageView imageView;
        private ImageLoader<T> imageLoader;
        private int placeholderResourceId;
        private Drawable placeholderDrawable;
        private ImageSetCallback imageSetCallback;
        private AssetPresenterCoordinator coordinator;

        public ImagePresenter<T> build(){
            assert imageView != null : "cannot create presenter without an image view";
            assert imageLoader != null : "cannot create presenter without an image loader";

            return new ImagePresenter<T>(this);
        }

        public Builder<T> setImageView(ImageView imageView) {
            this.imageView = imageView;
            return this;
        }

        public Builder<T> setImageLoader(ImageLoader<T> imageLoader) {
            this.imageLoader = imageLoader;
            return this;
        }

        public Builder<T> setPlaceholderResource(int resourceId) {
            assert resourceId == 0 || placeholderDrawable == null : "Can't set both a placeholder drawable and a placeholder resource";

            this.placeholderResourceId = resourceId;
            return this;
        }

        public Builder<T> setPlaceholderDrawable(Drawable placeholderDrawable) {
            assert placeholderDrawable == null || placeholderResourceId == 0 : "Can't set both a placeholder drawable and a placeholder resource";

            this.placeholderDrawable = placeholderDrawable;
            return this;
        }

        public Builder<T> setImageSetCallback(ImageSetCallback cb) {
            this.imageSetCallback = cb;
            return this;
        }

        public Builder<T> setAssetPresenterCoordinator(AssetPresenterCoordinator<T> coordinator) {
            this.coordinator = coordinator;
            return this;
        }
    }

    private final ImageLoader<T> imageLoader;
    private final Drawable placeholderDrawable;
    private final ImageSetCallback imageSetCallback;
    private final AssetPresenterCoordinator coordinator;
    protected final ImageView imageView;

    private int height = 0;
    private int width = 0;

    private T currentModel;
    private int currentCount;

    private boolean isImageSet;
    private boolean loadedFromCache = false;

    private boolean setLayoutListener = false;
    private final Runnable getDimens = new Runnable() {

        @Override
        public void run() {
            Log.d("AP: getDimens run width=" + width + " height=" + height);
            width = imageView.getWidth();
            height = imageView.getHeight();
            if (width == 0 || height == 0) {
                if (!setLayoutListener) {
                    imageView.getViewTreeObserver().addOnGlobalLayoutListener(new SizeObserver(imageView, ImagePresenter.this));
                    setLayoutListener = true;
                }
            } else if (pendingLoad != null) {
                imageView.post(pendingLoad);
            }
        }
    };
    private Runnable pendingLoad = null;

    public interface AssetPresenterCoordinator<T> {
        public boolean canSetImage(ImagePresenter<T> presenter);
        public boolean canSetPlaceholder(ImagePresenter<T> presenter);
    }

    private ImagePresenter(Builder<T> builder) {
        this.imageView = builder.imageView;
        this.imageLoader = builder.imageLoader;
        if (builder.placeholderResourceId != 0) {
            this.placeholderDrawable = imageView.getResources().getDrawable(builder.placeholderResourceId);
        } else {
            this.placeholderDrawable = builder.placeholderDrawable;
        }
        this.coordinator = builder.coordinator;
        this.imageSetCallback = builder.imageSetCallback;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setAssetModel(final T model) {
        if (model == null || model.equals(currentModel)) return;

        loadedFromCache = true;
        final int loadCount = ++currentCount;
        currentModel = model;
        isImageSet = false;

        if (width == 0 || height == 0) {
            pendingLoad = new Runnable() {
                @Override
                public void run() {
                    fetchPath(model, loadCount);
                }
            };
            getDimens();
        } else {
            fetchPath(model, loadCount);
        }

        loadedFromCache = false;

        if (!isImageSet()) {
            resetPlaceHolder();
        }
    }

    public void resetPlaceHolder() {
        if (placeholderDrawable == null || !canSetPlaceholder()) return;

        imageView.setImageDrawable(placeholderDrawable);
    }

    public void clear() {
        currentCount++;
        imageView.setImageBitmap(null);
        currentModel = null;
        isImageSet = false;
        imageLoader.clear();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private void fetchPath(final T model, final int loadCount) {
        pathToken = imageLoader.fetchPath(model, getWidth(), getHeight(), new ImageLoader.PathReadyCallback() {
            @Override
            public boolean onPathReady(String path) {
                if (loadCount != currentCount) return false;

                fetchImage(path, model, loadCount);
                return true;
            }

            @Override
            public void onError(Exception e) { }
        });
    }

    private void fetchImage(final String path, T model, final int loadCount) {
        imageToken = imageLoader.fetchImage(path, model, width, height, new ImageLoader.ImageReadyCallback() {
            @Override
            public boolean onImageReady(Bitmap image) {
                if (loadCount != currentCount || !canSetImage()) return false;

                if (imageSetCallback != null)
                    imageSetCallback.onImageSet(imageView, loadedFromCache);
                imageView.setImageBitmap(image);
                isImageSet = true;
                return true;
            }

            @Override
            public void onError(Exception e) { }
        });
    }

    private void getDimens() {
        imageView.post(getDimens);
    }

    protected boolean isImageSet() {
        return isImageSet;
    }

    private boolean canSetImage() {
        return coordinator == null || coordinator.canSetImage(this);
    }

    private boolean canSetPlaceholder() {
        return coordinator == null || coordinator.canSetPlaceholder(this);
    }

    private static class SizeObserver implements ViewTreeObserver.OnGlobalLayoutListener {

        private final WeakReference<ImageView> imageViewRef;
        private final WeakReference<ImagePresenter> imagePresenterRef;

        public SizeObserver(ImageView imageVew, ImagePresenter imagePresenter) {
            imageViewRef = new WeakReference<ImageView>(imageVew);
            imagePresenterRef = new WeakReference<ImagePresenter>(imagePresenter);
        }

        @Override
        public void onGlobalLayout() {
            ImageView imageView = imageViewRef.get();
            ImagePresenter presenter = imagePresenterRef.get();
            if (imageView != null && presenter != null && imageView.getWidth() > 0 && imageView.getHeight() > 0) {
                presenter.getDimens();
            }
        }
    }
}
