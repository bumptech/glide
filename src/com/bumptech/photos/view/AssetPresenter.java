/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.view;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;
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
public class AssetPresenter<T> {
    private int height = 0;
    private int width = 0;

    private Drawable placeholderDrawable;
    private ImageSetCallback imageSetCallback;

    private T currentModel;
    private int currentCount;

    private boolean isImageSet;
    protected final ImageView imageView;

    private final AssetPathConverter<T> assetIdToPath;
    private final ImageLoader imageLoader;
    private AssetPresenterCoordinator coordinator;

    public interface AssetPresenterCoordinator<T> {
        public boolean canSetImage(AssetPresenter<T> presenter);
        public boolean canSetPlaceholder(AssetPresenter<T> presenter);
    }

    public AssetPresenter(ImageView imageView, AssetPathConverter<T> assetIdToPath, ImageLoader imageLoader) {
        this.imageView = imageView;
        this.assetIdToPath = assetIdToPath;
        this.imageLoader = imageLoader;
    }

    public void setDimens(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setCoordinator(AssetPresenterCoordinator<T> controller) {
        this.coordinator = controller;
    }

    public void setPlaceholderDrawable(Drawable placeholderDrawable) {
        this.placeholderDrawable = placeholderDrawable;
    }

    public void setOnImageSetCallback(ImageSetCallback cb) {
        this.imageSetCallback = cb;
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

    public void onPathReady(String path, int loadCount) {
        if (loadCount != currentCount) return;

        imageLoader.loadImage(path, width, height, new ImageReadyCallback(this, loadCount));
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

    protected boolean isImageSet() {
        return isImageSet;
    }

    private boolean canSetImage() {
        return coordinator == null || coordinator.canSetImage(this);
    }

    private boolean canSetPlaceholder() {
        return coordinator == null || coordinator.canSetPlaceholder(this);
    }

    private static class ImageReadyCallback implements ImageLoader.ImageReadyCallback{

        private final WeakReference<AssetPresenter> assetPresenterRef;
        private final int loadCount;

        public ImageReadyCallback(AssetPresenter assetPresenter, int loadCount) {
            this.assetPresenterRef = new WeakReference<AssetPresenter>(assetPresenter);
            this.loadCount = loadCount;
        }

        @Override
        public void onImageReady() {
            final AssetPresenter assetPresenter = assetPresenterRef.get();
            if (assetPresenter != null ) {
                assetPresenter.onImageReady(loadCount);
            }
        }

        @Override
        public void onLoadFailed(Exception e) { }
    }

    private static class PathReadyCallback implements AssetPathConverter.PathReadyListener {

        private final int loadCount;
        private final WeakReference<AssetPresenter> assetPresenterRef;

        public PathReadyCallback(AssetPresenter assetPresenter, int loadCount) {
            this.assetPresenterRef = new WeakReference<AssetPresenter>(assetPresenter);
            this.loadCount = loadCount;
        }

        @Override
        public void onPathReady(String path) {
            final AssetPresenter assetPresenter = assetPresenterRef.get();
            if (assetPresenter != null) {
                assetPresenter.onPathReady(path, loadCount);
            }
        }
    }
}
