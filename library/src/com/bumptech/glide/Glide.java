package com.bumptech.glide;

import android.content.Context;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import com.bumptech.glide.loader.image.ImageLoader;
import com.bumptech.glide.loader.model.FileLoader;
import com.bumptech.glide.loader.model.ModelLoader;
import com.bumptech.glide.loader.model.UrlLoader;
import com.bumptech.glide.presenter.ImagePresenter;
import com.bumptech.glide.presenter.ImageSetCallback;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.loader.Approximate;
import com.bumptech.glide.resize.loader.CenterCrop;
import com.bumptech.glide.resize.loader.FitCenter;

import java.io.File;
import java.net.URL;

/**
 * Static helper methods/classes to present a simple unified interface for using glide. Allows 90%
 * of the functionality of the library. The trade off is some extra unused object allocation, and a few unavailable
 * methods. For many users this should be enough to make effective use of the library. For others it can serve as a
 * starting point and example. This class is not thread safe.
 */
public class Glide {
    private static final Glide GLIDE = new Glide();

    private ImageManager imageManager;

    public static Glide get() {
        return GLIDE;
    }

    @SuppressWarnings("unchecked")
    private static <T> ModelLoader<T> getModelFor(T model) {
        if (model == URL.class) {
            return (ModelLoader<T>) new UrlLoader();
        } else if (model == File.class) {
            return (ModelLoader<T>) new FileLoader();
        } else {
            throw new IllegalArgumentException("No default ModelLoader for class=" + model.getClass() +
                    ", you need to provide one by calling with()");
        }
    }

    protected Glide() { }

    /**
     * Return the current {@link ImageManager} or create and return a new one if one is not currently set.
     *
     * @see #setImageManager(com.bumptech.glide.resize.ImageManager.Builder)
     * @see #isImageManagerSet()
     *
     * @param context Any {@link Context}. This will not be retained passed this call
     * @return The current ImageManager
     */
    public ImageManager getImageManager(Context context) {
        if (!isImageManagerSet()) {
            setImageManager(new ImageManager.Builder(context));
        }
        return imageManager;
    }

    /**
     * Use to check whether or not an {@link ImageManager} has been set yet. Can be used in
     * {@link android.app.Activity#onCreate(android.os.Bundle)} along with
     * {@link #setImageManager(com.bumptech.glide.resize.ImageManager.Builder)} to set an {@link ImageManager} with
     * custom options for use with {@link Glide#load(Object)} and/or as an easily accessible singleton.
     *
     * @return true iff an {@link ImageManager} is currently set
     */
    public boolean isImageManagerSet() {
        return imageManager != null;
    }

    /**
     * @see #setImageManager(com.bumptech.glide.resize.ImageManager)
     *
     * @param builder The builder that will be used to construct a new ImageManager
     */
    public void setImageManager(ImageManager.Builder builder) {
        setImageManager(builder.build());
    }

    /**
     * Set the {@link ImageManager} to use with {@link Glide#load(Object)}. Replaces the current {@link ImageManager}
     * if one has already been set.
     *
     * @see #isImageManagerSet()
     *
     * @param imageManager The ImageManager to use
     */
    public void setImageManager(ImageManager imageManager) {
        this.imageManager = imageManager;
    }

    /**
     * A builder for a request
     *
     * @param <T> The type of the model the request will be built for
     */
    public static class HalfRequest<T> {
        private final T model;

        public HalfRequest(T model) {
            this.model = model;
        }

        /**
         * Build a request object for the given ImageView and model
         *
         * @param imageView The ImageView the request will be wrapping
         * @return A new {@link Request}
         */
        public Request<T> into(ImageView imageView) {
            if (imageView == null) {
                throw new IllegalArgumentException("ImageView can't be null");
            }
            return new Request<T>(model, imageView);
        }
    }

    /**
     * Manages building, tagging, retrieving and/or replacing an ImagePresenter for the given ImageView and model
     * @param <T> The type of model that will be loaded into the view
     */
    public static class Request<T> {
        private final T model;
        private final ImageView imageView;
        private final Context context;

        private ImagePresenter<T> presenter;
        private ImagePresenter.Builder<T> builder;
        private ModelLoader<T> modelLoader = null;

        @SuppressWarnings("unchecked")
        public Request(T model, ImageView imageView) {
            this.model = model;
            this.imageView = imageView;
            this.context = imageView.getContext();

            presenter = (ImagePresenter<T>) imageView.getTag(R.id.image_presenter_id);
            builder = new ImagePresenter.Builder<T>()
                    .setImageView(imageView)
                    .setImageLoader(new Approximate(getImageManager()));
        }

        /**
         * Set the {@link ModelLoader} for the model. For URL models, defaults to {@link UrlLoader},
         * for File models, defaults to {@link FileLoader}.
         *
         * @param modelLoader The {@link ModelLoader} to use. Replaces any existing loader
         * @return This Request
         */
        public Request<T> with(ModelLoader<T> modelLoader) {
            this.modelLoader = modelLoader;
            builder.setModelLoader(modelLoader);

            return this;
        }

        /**
         * Resizes models using {@link ImageManager#centerCrop(String, com.bumptech.glide.loader.stream.StreamLoader, int, int, com.bumptech.glide.resize.LoadedCallback)}
         * Replaces any existing resize style
         *
         * @return This Request
         */
        public Request<T> centerCrop() {
            return resizeWith(new CenterCrop(getImageManager()));
        }

        /**
         * Resizes models using {@link ImageManager#fitCenter(String, com.bumptech.glide.loader.stream.StreamLoader, int, int, com.bumptech.glide.resize.LoadedCallback)}
         * Replaces any existing resize style
         *
         * @return This Request
         */
        public Request<T> fitCenter() {
            return resizeWith(new FitCenter(getImageManager()));
        }

        /**
         * Resizes models using {@link ImageManager#getImageApproximate(String, com.bumptech.glide.loader.stream.StreamLoader, int, int, com.bumptech.glide.resize.LoadedCallback)}
         * Replaces any existing resize style
         *
         * @return This Request
         */
        public Request<T> approximate() {
            return resizeWith(new Approximate(getImageManager()));
        }

        /**
         * Set the {@link ImageLoader} to use to load images into memory
         *
         * @param imageLoader The {@link ImageLoader} to use
         * @return This Request
         */
        public Request<T> resizeWith(ImageLoader imageLoader) {
            if (presenter == null) {
                builder.setImageLoader(imageLoader);
            }

            return this;
        }

        /**
         * Sets an animation to run on the wrapped view when an image load finishes. Will only be run if the image
         * was loaded asynchronously (ie was not in the memory cache)
         *
         * @param animation The Animation to run
         * @return This Request
         */
        public Request<T> animate(final Animation animation) {
            builder.setImageSetCallback(new ImageSetCallback() {
                @Override
                public void onImageSet(ImageView view, boolean fromCache) {
                    view.clearAnimation();

                    if (!fromCache) {
                        view.startAnimation(animation);
                    }
                }
            });

            return this;
        }

        /**
         * @see #animate(android.view.animation.Animation)
         *
         * @param animationId The resource id of the animation to run
         * @return This Request
         */
        public Request<T> animate(int animationId) {
            return animate(AnimationUtils.loadAnimation(context, animationId));
        }

        /**
         * Creates an {@link ImagePresenter} or retrieves the existing one and starts loading the image represented by
         * the given model
         *
         * @see ImagePresenter#setModel(Object)
         */
        public void begin() {
            build();
            presenter.setModel(model);
        }

        private ImageManager getImageManager() {
            return GLIDE.getImageManager(context);
        }

        /**
         * Creates the new {@link ImagePresenter} if one does not currently exist for the current view and sets it as
         * the view's tag for the id {@code R.id.image_presenter_id}.
         *
         * If a Request is completed with replaceAndBuild(), then an ImagePresenter will be created  but the image
         * load will not actually be started until some subsequent Request is completed with {@link #begin()}
         */
        private void build() {
            if (presenter == null) {
                if (modelLoader == null) {
                    modelLoader = getModelFor(model);
                }
                presenter = builder.build();
                imageView.setTag(R.id.image_presenter_id, presenter);
            }
        }


    }

    /**
     * Begins constructing a load for a given model.
     *
     * @param model The model to load, must not be null
     * @param <T> The type of the model to load
     * @return A an unfinished Request that will be used to construct the components to load the model
     */
    public static <T> HalfRequest<T> load(T model) {
        if (model == null) {
            throw new IllegalArgumentException("Model can't be null");
        }

        return new HalfRequest<T>(model);
    }
}
