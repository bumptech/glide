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
 * Wraps an {@link android.widget.ImageView} to display arbitrary Bitmaps and provides a framework for fetching and loading bitmaps correctly
 * when views are being recycled. Uses {@link PathLoader} to download
 * an image or retrieve a path for a given model and {@link ImageLoader} to load
 * a bitmap from a given path and/or model. Also determines the actual width and height of the wrapped
 * {@link android.widget.ImageView} and passes that information to the provided
 * {@link PathLoader} and {@link ImageLoader}.
 *
 * @param <T> The type of the model that contains information necessary to display an image. Can be as simple
 *            as a String containing a path or a complex data type.
 */
public class ImagePresenter<T> {

    /**
     * A builder for an {@link ImagePresenter}. {@link Builder ImagePresenter.Builder#setImageView(android.widget.ImageView)},
     * {@link Builder ImagePresenter.Builder#setPathLoader}, and {@link Builder ImagePresenter.Builder#setImageLoader}
     * are required.
     *
     * @param <T> The type of the model that the presenter this builder will produce requires to load a path and an
     *           image from that path.
     */
    public static class Builder<T> {
        private ImageView imageView;
        private int placeholderResourceId;
        private Drawable placeholderDrawable;
        private ImageSetCallback imageSetCallback;
        private ImagePresenterCoordinator coordinator;
        private ImageLoader<T> imageLoader;
        private PathLoader<T> pathLoader;

        /**
         * Builds an ImagePresenter
         *
         * @return A new ImagePresenter
         */
        public ImagePresenter<T> build(){
            assert imageView != null : "cannot create presenter without an image view";
            assert imageLoader != null : "cannot create presenter without an image loader";
            assert pathLoader != null : "cannot create presenter without a path loader";

            return new ImagePresenter<T>(this);
        }

        /**
         * Required sets the {@link android.widget.ImageView} the presenter will use to display any loaded bitmaps
         *
         * @param imageView The {@link android.widget.ImageView} to wrap
         * @return This Builder object
         */
        public Builder<T> setImageView(ImageView imageView) {
            this.imageView = imageView;
            return this;
        }

        /**
         * Required sets the {@link com.bumptech.photos.loader.path.PathLoader} the presenter will use to download an
         * image or otherwise retrieve a path for a given T model
         *
         * @param pathLoader The {@link com.bumptech.photos.loader.path.PathLoader} to use to retrieve a path
         * @return This Builder
         */
        public Builder<T> setPathLoader(PathLoader<T> pathLoader) {
            this.pathLoader = pathLoader;
            return this;
        }

        /**
         * Required Sets the {@link com.bumptech.photos.loader.image.ImageLoader} the presenter will use to load a
         * Bitmap from the given path and/or model
         *
         * @param imageLoader The {@link com.bumptech.photos.loader.image.ImageLoader} to use to load an image
         * @return This Builder object
         */
        public Builder<T> setImageLoader(ImageLoader<T> imageLoader) {
            this.imageLoader = imageLoader;
            return this;
        }

        /**
         * Optional Sets a resource that will be displayed during loads and whenever
         * {@link ImagePresenter#resetPlaceHolder()} is called. Only call either this method or
         * {@link Builder ImagePresenter.Builder#setPlaceholderDrawable(android.graphics.drawable.Drawable)}, not both.
         *
         * @param resourceId The id of the resource to show
         * @return This Builder object
         */
        public Builder<T> setPlaceholderResource(int resourceId) {
            assert resourceId == 0 || placeholderDrawable == null : "Can't set both a placeholder drawable and a placeholder resource";

            this.placeholderResourceId = resourceId;
            return this;
        }

        /**
         * Optional Sets a drawable that will be displayed during loads and whenever
         * {@link ImagePresenter#resetPlaceHolder()} is called. Only call either this method or
         * {@link Builder ImagePresenter.Builder#setPlaceholderResource(int)}, not both.
         *
         * @param placeholderDrawable The drawable to show
         * @return This Builder object
         */
        public Builder<T> setPlaceholderDrawable(Drawable placeholderDrawable) {
            assert placeholderDrawable == null || placeholderResourceId == 0 : "Can't set both a placeholder drawable and a placeholder resource";

            this.placeholderDrawable = placeholderDrawable;
            return this;
        }

        /**
         * Optional Sets a callback that will be called after an image is loaded by
         * {@link com.bumptech.photos.loader.image.ImageLoader} and immediately before
         * {@link android.widget.ImageView#setImageBitmap(android.graphics.Bitmap)} is called by the presenter
         *
         * @param cb The callback to call
         * @return This Builder object
         */
        public Builder<T> setImageSetCallback(ImageSetCallback cb) {
            this.imageSetCallback = cb;
            return this;
        }

        /**
         * Optional Sets a coordinator that can allow or prevent placeholders or bitmaps from being set in otherwise
         * valid loads. See {@link com.bumptech.photos.presenter.ThumbImagePresenter}.
         *
         * @param coordinator The coordinator to set
         * @return This Builder object
         */
        public Builder<T> setImagePresenterCoordinator(ImagePresenterCoordinator<T> coordinator) {
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
    private final ImagePresenterCoordinator coordinator;
    protected final ImageView imageView;

    private int height = 0;
    private int width = 0;

    private Handler handler = new Handler();

    private T currentModel;
    private int currentCount;

    private boolean isImageSet = false;
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

    /**
     * An interface used to coordinate multiple {@link ImagePresenter} objects acting on the same view
     *
     * @param <T> The type of the {@link ImagePresenter} objects the implementation will be acting on
     */
    public interface ImagePresenterCoordinator<T> {

        /**
         * Determines if a presenter can display a loaded bitmap
         *
         * @param presenter The presenter requesting permission to display a bitmap
         * @return True iff the presenter can display a bitmap
         */
        public boolean canSetImage(ImagePresenter<T> presenter);

        /**
         * Determines if a presenter can display a placeholder
         *
         * @param presenter The presenter requesting permission to display a placeholder
         * @return True iff the presenter can display a placeholder
         */
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

    /**
     * A method to get the wrapped {@link android.widget.ImageView}. Note that setting any image or drawable on the view
     * directly may or may not be overridden at any point by the wrapper presenter.
     *
     * @return The {@link android.widget.ImageView} this {@link ImagePresenter} object wraps
     */
    public ImageView getImageView() {
        return imageView;
    }

    /**
     * Sets a model to load an image from. Each subsequent call will override all previous calls and will prevent any
     * bitmaps that are loaded from previous calls from being displayed even if the load completes successfully. Any
     * image being displayed at the time of this call will be replaced either by the placeholder or by the new image
     * if the load completes synchronously (ie it was in an in memory cache)
     *
     * <p>Note that a load will not begin before the ImagePresenter has determined the width and height of the wrapped
     * view, which can't happen until that view has been made visible and undergone layout out for the first time. Until
     * then the current load is stored. Subsequent calls will replace the stored load</p>
     *
     * @param model The model containing the information required to load a path and/or bitmap
     */
    public void setModel(final T model) {
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


    /**
     * Sets the placeholder as the current image for the {@link android.widget.ImageView}. Does not cancel any previous
     * loads, so the placeholder could be replaced with a loaded bitmap at any time. To cancel a load and display a
     * placeholder call {@link com.bumptech.photos.presenter.ImagePresenter#clear()}.
     */
    public void resetPlaceHolder() {
        if (!canSetPlaceholder()) return;

        imageView.setImageDrawable(placeholderDrawable);
    }

    /**
     * Prevents any bitmaps being loaded from previous calls to {@link ImagePresenter#setModel(Object)} from
     * being displayed and clears this presenter's {@link com.bumptech.photos.loader.image.ImageLoader} and
     * this presenter's {@link com.bumptech.photos.loader.path.PathLoader}. Also displays the current placeholder if
     * one is set
     */
    public void clear() {
        currentCount++;
        resetPlaceHolder();
        currentModel = null;
        isImageSet = false;
        pathLoader.clear();
        imageLoader.clear();
    }

    /**
     * Returns the current calculated width of the wrapped view. Will be 0 if the presenter has not yet calcualted a
     * width. May change at any time
     *
     * @return The width of the wrapped {@link android.widget.ImageView}
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the current calculated height of the wrapped view. Will be 0 if the presenter has not yet calculated a
     * height. May change at any time.
     *
     * @return The width of the wrapped {@link android.widget.ImageView }
     */
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
                if (loadCount != currentCount || !canSetImage() || image == null) return false;

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

    /**
     * For use primarily with {@link com.bumptech.photos.presenter.ImagePresenter.ImagePresenterCoordinator}
     *
     * @return True iff the wrapped {@link android.widget.ImageView} is displaying an image loaded by this
     *          {@link ImagePresenter}. False if the wrapped {@link android.widget.ImageView} is displaying a
     *          placeholder set by this presenter.
     */
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
