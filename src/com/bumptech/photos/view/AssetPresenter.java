/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.view;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import com.bumptech.photos.LoadedCallback;
import com.bumptech.photos.PhotoManager;
import com.bumptech.photos.view.assetpath.AssetPathConverter;
import com.bumptech.photos.view.loader.ImageLoader;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/25/12
 * Time: 10:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class AssetPresenter {
    private final PhotoManager photoManager;

    private int height = 0;
    private int width = 0;
    private Drawable placeholderDrawable;
    private ImageSetCallback fullSetCallback;

    private String fullAssetId;
    private boolean isImageSet;
    private Object fullToken;
    private int fullCount;
    protected final ImageView imageView;

    private final AssetPathConverter assetIdToPath;
    private Bitmap showing = null;
    private final ImageLoader imageLoader;

    public AssetPresenter(ImageView imageView, PhotoManager photoManager, AssetPathConverter assetIdToPath, ImageLoader imageLoader) {
        this.imageView = imageView;
        this.photoManager = photoManager;
        this.assetIdToPath = assetIdToPath;
        this.imageLoader = imageLoader;
    }

    public void setDimens(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setPlaceholderDrawable(Drawable placeholderDrawable) {
        this.placeholderDrawable = placeholderDrawable;
    }

    public void setOnImageSetCallback(ImageSetCallback cb) {
        this.fullSetCallback = cb;
    }

    public void setAssetId(final String assetId) {
        if (fullAssetId != null && fullAssetId.equals(assetId)) return;

        final int loadCount = ++fullCount;
        prepareResize(assetId);
        maybeCancelOldTask();

        if (assetId != null) {
            fetchPath(assetId, new AssetPathConverter.PathReadyListener() {
                @Override
                public void onPathReady(String path) {
                    if (loadCount == fullCount) {
                        fullToken = loadImage(path, imageLoader, fullResizeCallback(loadCount));
                    }
                }
            });
        }

        if (!isImageSet()) {
            resetPlaceHolder();
        }
    }

    public void resetPlaceHolder() {
        if (placeholderDrawable != null) {
            imageView.setImageDrawable(placeholderDrawable);
        }
    }

    public void clear() {
        fullCount++;
        maybeCancelOldTask();
        imageView.setImageBitmap(null);
        prepareResize(null);
    }

    protected boolean isImageSet() {
        return isImageSet;
    }

    protected void maybeCancelOldTask() {
        if (fullToken != null) {
            cancelTask(fullToken);
            fullToken = null;
        }
    }

    protected void cancelTask(Object token) {
        photoManager.cancelTask(token);
    }

    protected Object loadImage(String path, ImageLoader loader, LoadedCallback cb) {
        return loader.loadImage(photoManager, path, width, height, cb);
    }

    protected void fetchPath(String assetId, AssetPathConverter.PathReadyListener listener) {
        assetIdToPath.fetchPath(assetId, listener);
    }

    protected void updateAcquiredBitmap(Bitmap old, Bitmap next) {
        if (old != null) {
            photoManager.releaseBitmap(old);
        }
        if (next != null) {
            photoManager.acquireBitmap(next);
        }
    }

    private LoadedCallback fullResizeCallback(final int loadCount){
        return new LoadedCallback() {
            @Override
            public void onLoadCompleted(Bitmap loaded) {
                if (loadCount == fullCount) {
                    if (fullSetCallback != null)
                        fullSetCallback.onImageSet(imageView, false);
                    imageView.setImageBitmap(loaded);
                    isImageSet = true;

                    updateAcquiredBitmap(showing, loaded);
                    showing = loaded;
                }
            }

            @Override
            public void onLoadFailed(Exception e) {
                imageLoader.onLoadFailed(e);
            }
        };
    }

    private void prepareResize(String fullId) {
        fullAssetId = fullId;
        isImageSet = false;
    }
}
