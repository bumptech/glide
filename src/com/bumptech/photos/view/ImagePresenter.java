/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.view;

import android.graphics.drawable.Drawable;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import com.bumptech.photos.util.Log;
import com.bumptech.photos.view.assetpath.AssetPathConverter;
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
    public static class Builder<T> {
        private ImageView imageView;
        private AssetPathConverter<T> assetPathConverter;
        private ImageLoader imageLoader;
        private int placeholderResourceId;
        private Drawable placeholderDrawable;
        private ImageSetCallback imageSetCallback;
        private AssetPresenterCoordinator coordinator;

        public ImagePresenter<T> build(){
            assert imageView != null : "cannot create presenter without an image view";
            assert assetPathConverter != null : "cannot create presenter without an asset to path converter";
            assert imageLoader != null : "cannot create presenter without an image loader";

            return new ImagePresenter<T>(this);
        }

        public Builder<T> setImageView(ImageView imageView) {
            this.imageView = imageView;
            return this;
        }

        public Builder<T> setAssetPathConverter(AssetPathConverter<T> converter) {
            this.assetPathConverter = converter;
            return this;
        }

        public Builder<T> setImageLoader(ImageLoader imageLoader) {
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

    private final AssetPathConverter<T> assetIdToPath;
    private final ImageLoader imageLoader;
    private final Drawable placeholderDrawable;
    private final ImageSetCallback imageSetCallback;
    private final AssetPresenterCoordinator coordinator;
    protected final ImageView imageView;

    private int height = 0;
    private int width = 0;

    private T currentModel;
    private int currentCount;

    private boolean isImageSet;

    private boolean setLayoutListener = false;
    private final Runnable getDimens = new Runnable() {

        @Override
        public void run() {
            Log.d("AP: getDimens run width=" + width + " height=" + height);
            width = imageView.getWidth();
            height = imageView.getHeight();
            if ((width == 0 || height == 0) && !setLayoutListener) {
                imageView.getViewTreeObserver().addOnGlobalLayoutListener(new SizeObserver(imageView, ImagePresenter.this));
                setLayoutListener = true;
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
        this.assetIdToPath = builder.assetPathConverter;
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

        final int loadCount = ++currentCount;
        currentModel = model;
        isImageSet = false;

        assetIdToPath.fetchPath(model, new PathReadyCallback(this, loadCount));

        if (!isImageSet()) {
            resetPlaceHolder();
        }
    }

    public void onPathReady(final String path, final int loadCount) {
        if (loadCount != currentCount) return;

        if (width == 0 || height == 0) {
            pendingLoad = new Runnable() {
                @Override
                public void run() {
                    doLoad(path, loadCount);
                }
            };
            getDimens();
        } else {
            doLoad(path, loadCount);
        }
    }

    public void onImageReady(int loadCount) {
        if (loadCount != currentCount || !canSetImage()) return;

        if (imageSetCallback != null)
            imageSetCallback.onImageSet(imageView, false);
        imageView.setImageBitmap(imageLoader.getReadyBitmap());
        isImageSet = true;
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
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void getDimens() {
        imageView.post(getDimens);
    }

    protected boolean isImageSet() {
        return isImageSet;
    }

    private void doLoad(String path, int loadCount) {
        imageLoader.loadImage(path, width, height, new ImageReadyCallback(this, loadCount));
    }

    private boolean canSetImage() {
        return coordinator == null || coordinator.canSetImage(this);
    }

    private boolean canSetPlaceholder() {
        return coordinator == null || coordinator.canSetPlaceholder(this);
    }

    private static class ImageReadyCallback implements ImageLoader.ImageReadyCallback{

        private final WeakReference<ImagePresenter> assetPresenterRef;
        private final int loadCount;

        public ImageReadyCallback(ImagePresenter imagePresenter, int loadCount) {
            this.assetPresenterRef = new WeakReference<ImagePresenter>(imagePresenter);
            this.loadCount = loadCount;
        }

        @Override
        public void onImageReady() {
            final ImagePresenter imagePresenter = assetPresenterRef.get();
            if (imagePresenter != null ) {
                imagePresenter.onImageReady(loadCount);
            }
        }

        @Override
        public void onLoadFailed(Exception e) { }
    }

    private static class PathReadyCallback implements AssetPathConverter.PathReadyListener {

        private final int loadCount;
        private final WeakReference<ImagePresenter> assetPresenterRef;

        public PathReadyCallback(ImagePresenter imagePresenter, int loadCount) {
            this.assetPresenterRef = new WeakReference<ImagePresenter>(imagePresenter);
            this.loadCount = loadCount;
        }

        @Override
        public void onPathReady(String path) {
            final ImagePresenter imagePresenter = assetPresenterRef.get();
            if (imagePresenter != null) {
                imagePresenter.onPathReady(path, loadCount);
            }
        }
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
