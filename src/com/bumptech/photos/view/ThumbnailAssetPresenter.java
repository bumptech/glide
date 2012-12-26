/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.view;

import android.graphics.Bitmap;
import android.widget.ImageView;
import com.bumptech.photos.LoadedCallback;
import com.bumptech.photos.PhotoManager;
import com.bumptech.photos.view.assetpath.AssetPathConverter;
import com.bumptech.photos.view.loader.AsIs;
import com.bumptech.photos.view.loader.ImageLoader;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/25/12
 * Time: 10:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThumbnailAssetPresenter extends AssetPresenter {
    private ImageSetCallback thumbSetCallback = null;
    private String thumbAssetId = null;
    private Object thumbToken = null;
    private int thumbCount = 0;
    private final AsIs thumbLoader;
    private Bitmap currentThumbnail = null;

    public ThumbnailAssetPresenter(ImageView imageView, PhotoManager photoManager, AssetPathConverter assetIdToPath, ImageLoader imageLoader) {
        super(imageView, photoManager, assetIdToPath, imageLoader);
        thumbLoader = new AsIs();
    }

    public void setOnThumbSetCallback(ImageSetCallback cb) {
        this.thumbSetCallback = cb;
    }

    public void setAssetIds(String fullId, final String thumbId) {
        setAssetId(fullId);
        if (!isImageSet()) {
            final int loadCount = ++thumbCount;
            thumbAssetId = thumbId;
            if (thumbAssetId != null) {
                fetchPath(thumbId, new AssetPathConverter.PathReadyListener() {
                    @Override
                    public void onPathReady(String path) {
                        if (loadCount == thumbCount && !isImageSet()) {
                            thumbToken = loadImage(path, thumbLoader, thumbResizeCallback(loadCount));
                        }
                    }
                });
            }
        }
    }

    @Override
    public void clear() {
        thumbCount++;
        thumbAssetId = null;
        super.clear();
    }

    @Override
    protected void maybeCancelOldTask() {
        super.maybeCancelOldTask();
        if (thumbToken != null) {
            cancelTask(thumbToken);
            thumbToken = null;
        }
    }

    private LoadedCallback thumbResizeCallback(final int loadCount) {
        return new LoadedCallback() {
            @Override
            public void onLoadCompleted(Bitmap loaded) {
                if (loadCount == thumbCount && !isImageSet()) {
                    if (thumbSetCallback != null) {
                        thumbSetCallback.onImageSet(imageView, false);
                    }
                    imageView.setImageBitmap(loaded);

                    updateAcquiredBitmap(currentThumbnail, loaded);
                    currentThumbnail = loaded;
                }
            }

            @Override
            public void onLoadFailed(Exception e) { }
        };
    }
}
