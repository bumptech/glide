/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.view;

import android.graphics.drawable.Drawable;
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
    final Runnable getDimens = new Runnable() {
        @Override
        public void run() {
            width = imageView.getWidth();
            height = imageView.getHeight();
            Log.d("AP: getDimens run width=" + width + " height=" + height);
            if (pendingLoad != null) {
                if (width != 0 && height != 0) {
                    imageView.post(pendingLoad);
                    pendingLoad = null;
                } else {
                    imageView.postDelayed(getDimens, 50);
                }
            }
        }
    };
    private Runnable pendingLoad = null;

    public interface AssetPresenterCoordinator<T> {
        public boolean canSetImage(ImagePresenter<T> presenter);
        public boolean canSetPlaceholder(ImagePresenter<T> presenter);
    }

    public ImagePresenter(final ImageView imageView, AssetPathConverter<T> assetIdToPath, final ImageLoader imageLoader) {
        this.imageView = imageView;
        this.assetIdToPath = assetIdToPath;
        this.imageLoader = imageLoader;
    }

    public void setCoordinator(AssetPresenterCoordinator<T> controller) {
        this.coordinator = controller;
    }

    public void setPlaceholderDrawable(Drawable placeholderDrawable) {
        this.placeholderDrawable = placeholderDrawable;
    }

    public void setPlaceholderResource(int resourceId) {
        this.placeholderDrawable = imageView.getResources().getDrawable(resourceId);
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

    public void onPathReady(final String path, final int loadCount) {
        if (loadCount != currentCount) return;

        if (width == 0 || height == 0) {
            pendingLoad = new Runnable() {
                @Override
                public void run() {
                    doLoad(path, loadCount);
                }
            };
            imageView.post(getDimens);
        } else {
            doLoad(path, loadCount);
        }
    }

    private void doLoad(String path, int loadCount) {
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
}
