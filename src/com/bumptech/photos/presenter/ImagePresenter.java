/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.presenter;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import com.bumptech.photos.util.Log;
import com.bumptech.photos.loader.path.PathLoader;
import com.bumptech.photos.loader.image.ImageLoader;

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
        private int placeholderResourceId;
        private Drawable placeholderDrawable;
        private ImageSetCallback imageSetCallback;
        private AssetPresenterCoordinator coordinator;
        private ImageLoader<T> imageLoader;
        private PathLoader<T> pathLoader;

        public ImagePresenter<T> build(){
            assert imageView != null : "cannot create presenter without an image view";
            assert imageLoader != null : "cannot create presenter without an image loader";
            assert pathLoader != null : "cannot create presenter without a path loader";

            return new ImagePresenter<T>(this);
        }

        public Builder<T> setImageView(ImageView imageView) {
            this.imageView = imageView;
            return this;
        }

        public Builder<T> setPathLoader(PathLoader<T> pathLoader) {
            this.pathLoader = pathLoader;
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

    private static final String PENDING_LOAD_TOKEN = "pending_load";
    private static final int PENDING_LOAD_DELAY = 100; //60 fps = 1000/60 = 16.67 ms

    private Object pathToken;
    private Object imageToken;

    private final PathLoader<T> pathLoader;
    private final ImageLoader<T> imageLoader;
    private final Drawable placeholderDrawable;
    private final ImageSetCallback imageSetCallback;
    private final AssetPresenterCoordinator coordinator;
    protected final ImageView imageView;

    private int height = 0;
    private int width = 0;

    private Handler handler = new Handler();

    private T currentModel;
    private int currentCount;

    private boolean isImageSet;
    private boolean loadedFromCache = false;

    private final Runnable getDimens = new Runnable() {
        @Override
        public void run() {
            if (imageView.getWidth() == width && imageView.getHeight() == height) return;

            width = imageView.getWidth();
            height = imageView.getHeight();
            if (pendingLoad != null)
                Log.d("IP: getDimens width=" + width + " height=" + height);
            if (width != 0 && height != 0) {
                postPendingLoad();
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
        this.pathLoader = builder.pathLoader;
        if (builder.placeholderResourceId != 0) {
            this.placeholderDrawable = imageView.getResources().getDrawable(builder.placeholderResourceId);
        } else {
            this.placeholderDrawable = builder.placeholderDrawable;
        }
        this.coordinator = builder.coordinator;
        this.imageSetCallback = builder.imageSetCallback;
        imageView.getViewTreeObserver().addOnGlobalLayoutListener(new SizeObserver(imageView, ImagePresenter.this));
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
                    Log.d("IP: pendingLoad run width=" + width + " height=" + height);
                    fetchPath(model, loadCount);
                    pendingLoad = null;
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
        if (!canSetPlaceholder()) return;

        imageView.setImageDrawable(placeholderDrawable);
    }

    public void clear() {
        currentCount++;
        resetPlaceHolder();
        currentModel = null;
        isImageSet = false;
        pathLoader.clear();
        imageLoader.clear();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private void postPendingLoad() {
        if (pendingLoad == null) return;

        //If an image view is actively changing sizes, we want to delay our resize job until
        //the size has stabilized so that the image we load will match the final size, rather than some
        //size part way through the change. One example of this is as part of an animation where a view is
        //expanding or shrinking
        handler.removeCallbacksAndMessages(PENDING_LOAD_TOKEN);
        handler.postAtTime(pendingLoad, PENDING_LOAD_TOKEN, SystemClock.uptimeMillis() + PENDING_LOAD_DELAY);
    }

    private void fetchPath(final T model, final int loadCount) {
        pathToken = pathLoader.fetchPath(model, getWidth(), getHeight(), new PathLoader.PathReadyCallback() {
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
