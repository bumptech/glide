/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.presenter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import com.bumptech.glide.loader.image.ImageLoader;
import com.bumptech.glide.loader.model.ModelLoader;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.loader.transformation.None;
import com.bumptech.glide.loader.transformation.TransformationLoader;
import com.bumptech.glide.presenter.target.ImageViewTarget;
import com.bumptech.glide.presenter.target.Target;
import com.bumptech.glide.resize.load.Transformation;
import com.bumptech.glide.util.Log;

/**
 * Wraps an {@link Target} to display arbitrary Bitmaps and provides a framework for fetching and
 * loading bitmaps correctly when targest are being recycled. Uses {@link ModelLoader} to between a
 * model {@link StreamLoader} for a given model, a {@link StreamLoader} to download or otherwise obtain an
 * {@link java.io.InputStream} for a given model, and  an {@link ImageLoader} to load a bitmap from a given
 * {@link java.io.InputStream}. This class also determines the width and height of the wrapped
 * {@link android.widget.ImageView} at runtime and passes that information to the provided {@link ModelLoader} and
 * {@link ImageLoader}.
 *
 * @param <T> The type of the model that contains information necessary to display an image. Can be as simple
 *            as a String containing a path or a complex data type.
 */
public class ImagePresenter<T> {

    /**
     * A builder for an {@link ImagePresenter}.
     *
     * <p> {@link Builder ImagePresenter.Builder#setImageView(android.widget.ImageView) setImageView},
     * {@link Builder ImagePresenter.Builder#setPathLoader setPathLoader}, and
     * {@link Builder ImagePresenter.Builder#setImageLoader setIamgeLoader}
     * are required.
     * </p>
     *
     * @param <T> The type of the model that the presenter this builder will produce requires to load a path and an
     *           image from that path.
     */
    @SuppressWarnings("unused")
    public static class Builder<T> {
        private int placeholderResourceId;
        private Drawable placeholderDrawable;
        private ImageReadyCallback imageReadyCallback;
        private ImagePresenterCoordinator coordinator;
        private ImageLoader imageLoader;
        private Context context;
        private ExceptionHandler<T> exceptionHandler = new ExceptionHandler<T>() {
            @Override
            public void onException(Exception e, T model, boolean isCurrent) {
                if (e != null) {
                    e.printStackTrace();
                } else {
                    Log.e("IP: onImageLoadException model= " + model);
                }
            }
        };

        private ModelLoader<T> modelLoader;
        private int errorResourceId;
        private Drawable errorDrawable;
        private TransformationLoader<T> transformationLoader;
        private Target target;

        /**
         * Builds an ImagePresenter.
         *
         * <p>
         *     Note - this ImagePresenter will not be retained by default. Consider
         *     {@link android.view.View#setTag(Object)}, or the view holder pattern.
         * </p>
         *
         * @return A new ImagePresenter
         */
        public ImagePresenter<T> build(){
            if (target == null) {
                throw new IllegalArgumentException("cannot create presenter without a target");
            }
            if (imageLoader == null) {
                throw new IllegalArgumentException("cannot create presenter without an image loader");
            }
            if (modelLoader == null) {
                throw new IllegalArgumentException("cannot create presenter without a model loader");
            }

            if (transformationLoader == null) {
                transformationLoader = new None<T>();
            }

            return new ImagePresenter<T>(this);
        }

        /**
         * Required - Sets the {@link android.widget.ImageView} the presenter will use to display any loaded bitmaps.
         * Alternatively you can instead set a more general target for an object other than an ImageView using
         * {@link #setTarget(com.bumptech.glide.presenter.target.Target, android.content.Context)}.
         *
         * @param imageView The {@link android.widget.ImageView} to wrap
         * @return This Builder object
         */
        public Builder<T> setImageView(final ImageView imageView) {
            setTarget(new ImageViewTarget(imageView), imageView.getContext());

            return this;
        }

        /**
         * Required - Sets the {@link Target} the presenter will use to display any loaded bitmaps. If you are loading
         * bitmaps into an {@link ImageView}, you can instead use {@link #setImageView(android.widget.ImageView)}.
         *
         * @param target The {@link Target} to wrap
         * @param context A context that can be held for the duration of the load
         * @return This builder object
         */
        public Builder<T> setTarget(Target target, Context context) {
            this.target = target;
            this.context = context;

            return this;
        }

        /**
         * Required - Sets the {@link ModelLoader} the presenter will use to obtain an id for and an InputStream to the
         * image represented by a given model
         *
         * @param modelLoader The {@link ModelLoader} to use to obtain the id and InputStreams
         * @return This Builder object
         */
        public Builder<T> setModelLoader(ModelLoader<T> modelLoader) {
            this.modelLoader = modelLoader;
            return this;
        }

        /**
         * Required - Sets the {@link com.bumptech.glide.loader.image.ImageLoader} the presenter will use to load a
         * Bitmap from the given path and/or model
         *
         * @param imageLoader The {@link com.bumptech.glide.loader.image.ImageLoader} to use to load an image
         * @return This Builder object
         */
        public Builder<T> setImageLoader(ImageLoader imageLoader) {
            this.imageLoader = imageLoader;
            return this;
        }

        /**
         * Optional - Sets a resource that will be displayed during loads and whenever
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
         * Optional - Sets a drawable that will be displayed during loads and whenever
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
         * Optional - Sets a resource that will be displayed whenever a load fails. Only call either this method
         * or {@link #setErrorDrawable(android.graphics.drawable.Drawable)}, not both.
         *
         * @param resourceId The id of the resource to show
         * @return This Builder object
         */
        public Builder<T> setErrorResource(int resourceId) {
            if (resourceId != 0 && errorDrawable != null) {
                throw new IllegalArgumentException("Can't set both an error drawable and an error resource");
            }

            this.errorResourceId = resourceId;
            return this;
        }

        /**
         * Optional - Sets a drawable that will be displayed whenever a load fails. Only call either this or
         * {@link #setErrorResource(int)}, not both.
         *
         * @param drawable The drawable to show
         * @return This Builder object
         */
        public Builder<T> setErrorDrawable(Drawable drawable) {
            if (errorResourceId != -1 && drawable != null) {
                throw new IllegalArgumentException("Can't set both an error drawable and an error resource");
            }

            this.errorDrawable = drawable;
            return this;
        }

        /**
         * Optional - Sets a callback that will be called after an image is loaded by
         * {@link com.bumptech.glide.loader.image.ImageLoader} and immediately before
         * {@link android.widget.ImageView#setImageBitmap(android.graphics.Bitmap)} is called by the presenter
         *
         * @param cb The callback to call
         * @return This Builder object
         */
        public Builder<T> setImageReadyCallback(ImageReadyCallback cb) {
            this.imageReadyCallback = cb;
            return this;
        }

        /**
         * Optional - Sets a coordinator that can allow or prevent placeholders or bitmaps from being set in otherwise
         * valid loads.
         *
         * @see com.bumptech.glide.presenter.ThumbImagePresenter
         *
         * @param coordinator The coordinator to set
         * @return This Builder object
         */
        public Builder<T> setImagePresenterCoordinator(ImagePresenterCoordinator coordinator) {
            this.coordinator = coordinator;
            return this;
        }

        /**
         * Optional - Sets a handler that will be notified if any path or image load causes an exception.
         *
         * @see com.bumptech.glide.presenter.ImagePresenter.ExceptionHandler
         *
         * @param exceptionHandler The exception handler to set
         * @return This builder object
         */
        public Builder<T> setExceptionHandler(ExceptionHandler<T> exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        /**
         * Optional - Sets a transformation loader to use to obtain a transformation to apply to images on a per
         * model basis.
         *
         * @see Transformation
         *
         * @param transformationLoader A {@link TransformationLoader} for this model type
         * @return This builder object
         */
        public Builder<T> setTransformationLoader(TransformationLoader<T> transformationLoader) {
            this.transformationLoader = transformationLoader;
            return this;
        }
    }

    @SuppressWarnings("all")
    private Object imageToken; //this is just a reference we may need to keep, otherwise unused

    private final Target target;

    private final ModelLoader<T> modelLoader;
    private final ImageLoader imageLoader;
    private final TransformationLoader<T> transformationLoader;

    private final Drawable placeholderDrawable;
    private final ImageReadyCallback imageReadyCallback;
    private final ImagePresenterCoordinator coordinator;
    private final ExceptionHandler<T> exceptionHandler;

    private T currentModel;
    private int currentCount;

    private boolean isImageSet = false;
    private boolean loadedFromCache = false;
    private final Drawable errorDrawable;

    /**
     * An interface used to coordinate multiple {@link ImagePresenter} objects acting on the same view
     */
    public interface ImagePresenterCoordinator {

        /**
         * Determines if a presenter can display a loaded bitmap
         *
         * @param presenter The presenter requesting permission to display a bitmap
         * @return True iff the presenter can display a bitmap
         */
        public boolean canSetImage(ImagePresenter presenter);

        /**
         * Determines if a presenter can display a placeholder
         *
         * @param presenter The presenter requesting permission to display a placeholder
         * @return True iff the presenter can display a placeholder
         */
        public boolean canSetPlaceholder(ImagePresenter presenter);
    }

    /**
     * An interface for logging or otherwise handling exceptions that may occur during an image load
     *
     * @param <T> The type of the model
     */
    public interface ExceptionHandler<T> {
        /**
         * Called whenever a load causes an exception
         *
         * @param e The exception
         * @param model The model that was being loaded
         * @param isCurrent true iff the presenter currently wants to display the image from the load that failed
         */
        public void onException(Exception e, T model, boolean isCurrent);
    }

    protected ImagePresenter(Builder<T> builder) {
        this.imageLoader = builder.imageLoader;
        this.transformationLoader = builder.transformationLoader;

        final Resources res = builder.context.getResources();
        if (builder.placeholderResourceId != 0) {
            this.placeholderDrawable = res.getDrawable(builder.placeholderResourceId);
        } else {
            this.placeholderDrawable = builder.placeholderDrawable;
        }
        if (builder.errorResourceId != 0){
            this.errorDrawable = res.getDrawable(builder.errorResourceId);
        } else {
            this.errorDrawable = builder.errorDrawable;
        }

        this.coordinator = builder.coordinator;
        this.imageReadyCallback = builder.imageReadyCallback;
        this.exceptionHandler = builder.exceptionHandler;
        this.modelLoader = builder.modelLoader;
        this.target = builder.target;
    }

    /**
     * Sets a model to load an image from. Each subsequent call will override all previous calls and will prevent any
     * bitmaps that are loaded from previous calls from being displayed even if the load completes successfully. Any
     * image being displayed at the time of this call will be replaced either by the placeholder or by the new image
     * if the load completes synchronously (ie it was in an in memory cache)
     *
     * <p>
     *     Note - A load will not begin before the ImagePresenter has determined the width and height of the wrapped
     * view, which can't happen until that view has been made visible and undergone layout for the first time. Until
     * then the current load is stored. Subsequent calls will replace the stored load.
     * </p>
     *
     * @param model The model containing the information required to load a path and/or bitmap
     */
    public void setModel(final T model) {
        if (model == null) {
            clear();
        } else if (!model.equals(currentModel)) {
            loadedFromCache = true;
            final int loadCount = ++currentCount;
            currentModel = model;
            isImageSet = false;

            target.getSize(new Target.SizeReadyCallback() {
                @Override
                public void onSizeReady(int width, int height) {
                    fetchImage(model, width, height, loadCount);
                }
            });

            loadedFromCache = false;

            if (!isImageSet) {
                resetPlaceHolder();
            }
        }
    }

    /**
     * Sets the placeholder as the current image for the {@link android.widget.ImageView}. Does not cancel any previous
     * loads, so the placeholder could be replaced with a loaded bitmap at any time. To cancel a load and display a
     * placeholder call {@link #clear()}.
     */
    public void resetPlaceHolder() {
        if (!canSetPlaceholder()) return;

        target.setPlaceholder(placeholderDrawable);
    }

    /**
     * Prevents any bitmaps being loaded from previous calls to {@link ImagePresenter#setModel(Object) setModel} from
     * being displayed and clears this presenter's {@link ImageLoader} and
     * this presenter's {@link ModelLoader}. Also displays the current placeholder if
     * one is set
     */
    public void clear() {
        currentCount++;
        resetPlaceHolder();
        currentModel = null;
        isImageSet = false;
        imageLoader.clear();
    }

    private void fetchImage(final T model, int width, int height, final int loadCount) {
        imageLoader.clear();
        final String id = modelLoader.getId(model);
        final StreamLoader sl = modelLoader.getStreamLoader(model, width, height);
        final Transformation t = transformationLoader.getTransformation(model);

        imageToken = imageLoader.fetchImage(id, sl, t, width, height, new ImageLoader.ImageReadyCallback() {
            @Override
            public boolean onImageReady(Bitmap image) {
                if (loadCount != currentCount || !canSetImage() || image == null) return false;

                if (imageReadyCallback != null)
                    imageReadyCallback.onImageReady(target, loadedFromCache);
                target.onImageReady(image);
                isImageSet = true;
                return true;
            }

            @Override
            public void onException(Exception e) {
                final boolean relevant = loadCount == currentCount;
                if (exceptionHandler != null) {
                    exceptionHandler.onException(e, model, relevant);
                }
                if (relevant && canSetPlaceholder() && errorDrawable != null) {
                    target.setPlaceholder(errorDrawable);
                }
            }
        });
    }

    /**
     * For use primarily with {@link com.bumptech.glide.presenter.ImagePresenter.ImagePresenterCoordinator}
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
}
