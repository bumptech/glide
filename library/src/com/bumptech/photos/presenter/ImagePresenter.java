/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.presenter;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import com.bumptech.photos.loader.image.ImageLoader;
import com.bumptech.photos.loader.path.PathLoader;

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
        private ExceptionHandler<T> exceptionHandler;

        /**
         * Builds an ImagePresenter
         *
         * @return A new ImagePresenter
         */
        public ImagePresenter<T> build(){
            if (imageView == null) {
                throw new IllegalArgumentException("cannot create presenter without an image view");
            }
            if (imageLoader == null) {
                throw new IllegalArgumentException("cannot create presenter without an image loader");
            }
            if (pathLoader == null) {
                throw new IllegalArgumentException("cannot create presenter without a path loader");

            }

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
            if (resourceId != 0 && placeholderDrawable != null) {
                throw new IllegalArgumentException("Can't set both a placeholder drawable and a placeholder resource");
            }

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
            if (placeholderDrawable != null && placeholderResourceId != 0) {
                throw new IllegalArgumentException("Can't set both a placeholder drawable and a placeholder resource");
            }

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

        /**
         * Optional Sets a handler that will be notified if any path or image load causes an exception.
         * See {@link com.bumptech.photos.presenter.ImagePresenter.ExceptionHandler}.
         *
         * @param exceptionHandler The exception handler to set
         * @return This builder object
         */
        public Builder<T> setExceptionHandler(ExceptionHandler<T> exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }
    }

    private Object pathToken;
    private Object imageToken;

    private final PathLoader<T> pathLoader;
    private final ImageLoader<T> imageLoader;
    private final Drawable placeholderDrawable;
    private final ImageSetCallback imageSetCallback;
    private final ImagePresenterCoordinator coordinator;
    private final ExceptionHandler<T> exceptionHandler;
    protected final ImageView imageView;

    private T currentModel;
    private int currentCount;

    private boolean isImageSet = false;
    private boolean loadedFromCache = false;
    private final SizeDeterminer sizeDeterminer;

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

    public interface ExceptionHandler<T> {
        public void onPathLoadException(Exception e, T model, boolean isCurrent);
        public void onImageLoadException(Exception e, T model, String path, boolean isCurrent);
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
        this.exceptionHandler = builder.exceptionHandler;
        sizeDeterminer = new SizeDeterminer(imageView);
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

        sizeDeterminer.getSize(new SizeDeterminer.SizeReadyCallback() {
            @Override
            public void onSizeReady(int width, int height) {
                fetchPath(model, width, height, loadCount);
            }
        });

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

    private void fetchPath(final T model, final int width, final int height, final int loadCount) {
        pathToken = pathLoader.fetchPath(model, width, height, new PathLoader.PathReadyCallback() {
            @Override
            public boolean onPathReady(final String path) {
                if (loadCount != currentCount) return false;
                fetchImage(path, model, width, height, loadCount);

                return true;
            }

            @Override
            public void onException(Exception e) {
                if (exceptionHandler != null) {
                    exceptionHandler.onPathLoadException(e, model, loadCount == currentCount);
                }
            }
        });
    }

    private void fetchImage(final String path, final T model, int width, int height, final int loadCount) {
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
            public void onException(Exception e) {
                if (exceptionHandler != null) {
                    exceptionHandler.onImageLoadException(e, model, path, loadCount == currentCount);
                }
            }
        });
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

    private static class SizeDeterminer {
        private static final String PENDING_SIZE_CHANGE_TOKEN = "pending_load";
        private static final int PENDING_SIZE_CHANGE_DELAY = 100; //60 fps = 1000/60 = 16.67 ms

        private final View view;
        private int width = 0;
        private int height = 0;
        private boolean valid = false;
        private SizeReadyCallback cb = null;
        private final Runnable getDimens = new Runnable() {
            @Override
            public void run() {
                if (cb == null) return;

                ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                if (layoutParams.width > 0 && layoutParams.height > 0) {
                    cb.onSizeReady(layoutParams.width, layoutParams.height);
                } else if (view.getWidth() > 0 && view.getHeight() > 0) {
                    valid = true;
                    width = view.getWidth();
                    height = view.getHeight();
                    cb.onSizeReady(width, height);
                    cb = null;
                }
            }
        };

        private static class SizeObserver implements ViewTreeObserver.OnGlobalLayoutListener {
            private final WeakReference<SizeDeterminer> sizeDeterminerRef;
            private Handler handler = new Handler();

            public SizeObserver(SizeDeterminer sizeDeterminer) {
                this.sizeDeterminerRef = new WeakReference<SizeDeterminer>(sizeDeterminer);
            }

            @Override
            public void onGlobalLayout() {
                final SizeDeterminer sizeDeterminer = sizeDeterminerRef.get();
                if (sizeDeterminer != null) {
                    handler.removeCallbacksAndMessages(PENDING_SIZE_CHANGE_TOKEN);
                    handler.postAtTime(new Runnable() {
                        @Override
                        public void run() {
                            sizeDeterminer.maybeInvalidate();
                        }
                    }, PENDING_SIZE_CHANGE_TOKEN, SystemClock.uptimeMillis() + PENDING_SIZE_CHANGE_DELAY);
                }
            }
        }

        public interface SizeReadyCallback {
            public void onSizeReady(int width, int height);
        }

        public SizeDeterminer(View view) {
            this.view = view;
            view.getViewTreeObserver().addOnGlobalLayoutListener(new SizeObserver(this));
        }

        public void getSize(SizeReadyCallback cb) {
            this.cb = null;
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams.width > 0 && layoutParams.height > 0) {
                cb.onSizeReady(layoutParams.width, layoutParams.height);
            } else if (valid) {
                cb.onSizeReady(width, height);
            } else {
                this.cb = cb;
                view.removeCallbacks(getDimens);
                view.post(getDimens);
            }
        }

        private void maybeInvalidate() {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams.width <= 0 || layoutParams.height <= 0) {
                if (view.getWidth() >= 0 && view.getHeight() >= 0) {
                    width = view.getWidth();
                    height = view.getHeight();
                    valid = true;
                    if (cb != null) {
                        cb.onSizeReady(width, height);
                        cb = null;
                    }
                } else {
                    valid = false;
                }
            }
        }
    }
}
