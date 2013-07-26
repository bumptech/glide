package com.bumptech.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.loader.image.ImageLoader;
import com.bumptech.glide.loader.model.DrawableLoader;
import com.bumptech.glide.loader.model.FileLoader;
import com.bumptech.glide.loader.model.ModelLoader;
import com.bumptech.glide.loader.model.StringLoader;
import com.bumptech.glide.loader.model.UriLoader;
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
 * A singleton to present a simple static interface for Glide {@link Glide.Request} and to create and manage an
 * {@link ImageLoader} and {@link com.android.volley.RequestQueue}. This class provides most of the functionality of
 * {@link ImagePresenter} with a simpler but less efficient interface. For more complicated cases it may be worth
 * considering using {@link ImagePresenter} and {@link com.bumptech.glide.presenter.ImagePresenter.Builder} directly.
 *
 * <p>
 * Note - This class is not thread safe.
 * </p>
 */
public class Glide {
    private static final Glide GLIDE = new Glide();

    private ImageManager imageManager = null;
    private RequestQueue requestQueue = null;

    /**
     * Get the singleton.
     *
     * @return the singleton
     */
    public static Glide get() {
        return GLIDE;
    }

    protected Glide() { }

    /**
     * Return the current {@link RequestQueue} or create and return a new one if one is not currently set
     *
     * @see #setRequestQueue(RequestQueue)
     * @see #isRequestQueueSet()
     *
     * @param context A context to use for Volley
     * @return The {@link RequestQueue}
     */
    public RequestQueue getRequestQueue(Context context) {
        if (!isRequestQueueSet()) {
            setRequestQueue(Volley.newRequestQueue(context));
        }
        return requestQueue;
    }

    /**
     * Use to check whether or not an {@link RequestQueue} has been set yet. Can be used in
     * {@link android.app.Activity#onCreate(android.os.Bundle) Activity.onCreate} along with
     * {@link #setRequestQueue(RequestQueue) setRequestQueue} to set a {@link RequestQueue} with custom options
     * for use with {@link Glide#load(Object) load} and/or as an easily accessible singleton
     *
     * @return true iff a {@link RequestQueue} has already been set
     */
    public boolean isRequestQueueSet() {
        return requestQueue != null;
    }

    /**
     * Set the {@link RequestQueue} to use with {@link Glide#load(Object)} load}. Replaces the current
     * {@link RequestQueue} if one has already been set
     *
     * @param requestQueue The {@link RequestQueue} to set
     */
    public void setRequestQueue(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

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
     * {@link android.app.Activity#onCreate(android.os.Bundle) Activity.onCreate} along with
     * {@link #setImageManager(com.bumptech.glide.resize.ImageManager.Builder) setImageManager} to set an
     * {@link ImageManager} with custom options for use with {@link Glide#load(Object) load} and/or as an easily
     * accessible singleton.
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
     * Set the {@link ImageManager} to use with {@link Glide#load(Object) load}. Replaces the current
     * {@link ImageManager} if one has already been set.
     *
     * @see #isImageManagerSet()
     *
     * @param imageManager The ImageManager to use
     */
    public void setImageManager(ImageManager imageManager) {
        this.imageManager = imageManager;
    }

    /**
     * Begins constructing a load for a given model.
     *
     * <p>
     * Only certain models are supported by default. See
     * {@link Glide.Request#with(com.bumptech.glide.loader.model.ModelLoader)} for models with default a
     * {@link ModelLoader}.
     * </p>
     *
     * <p>
     * Note - If an {@link ImageManager} has not yet been set via
     * {@link #setImageManager(ImageManager) setImageManager}, one will be created during this call unless
     * you specify a {@link ImageLoader} that does not use {@link #getRequestQueue(android.content.Context)
     * getRequestQueue} via {@link Glide.Request#resizeWith(ImageLoader) resizeWith}
     * </p>
     *
     * <p>
     * Note - If the model is a {@link URL} and an {@link com.android.volley.RequestQueue} has not yet been set via
     * {@link #setRequestQueue(com.android.volley.RequestQueue) setRequestQueue}, one will be created during this call
     * unless you specify a {@link ModelLoader} via {@link Glide.Request#with(ModelLoader) with}.
     * </p>
     *
     * @see #setImageManager(com.bumptech.glide.resize.ImageManager)
     * @see #setRequestQueue(com.android.volley.RequestQueue)
     * @see #isImageManagerSet()
     * @see #isRequestQueueSet()
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

    @SuppressWarnings("unchecked")
    private static <T> ModelLoader<T> getModelFor(T model, Context context) {
        final ModelLoader result;
        if (model instanceof URL) {
            result = new UrlLoader(GLIDE.getRequestQueue(context));
        } else if (model instanceof File) {
            result = new FileLoader(context);
        } else if (model instanceof Uri) {
            result = new UriLoader(context, new UrlLoader(GLIDE.getRequestQueue(context)));
        } else if (model instanceof String) {
            result = new StringLoader(new UriLoader(context, new UrlLoader(GLIDE.getRequestQueue(context))));
        } else if (model instanceof Integer) {
            result = new DrawableLoader(context);
        } else {
            throw new IllegalArgumentException("No default ModelLoader for class=" + model.getClass() +
                    ", you need to provide one by calling with()");
        }
        return result;
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
     *
     * @param <T> The type of model that will be loaded into the view
     */
    @SuppressWarnings("unused") //public api
    public static class Request<T> {
        private final T model;
        private final Context context;

        private ImagePresenter<T> presenter;
        private ImagePresenter.Builder<T> builder;
        private ModelLoader<T> modelLoader = null;

        @SuppressWarnings("unchecked")
        public Request(T model, ImageView imageView) {
            this.model = model;
            this.context = imageView.getContext();

            presenter = ImagePresenter.getCurrent(imageView);
            builder = new ImagePresenter.Builder<T>()
                    .setImageView(imageView)
                    .setImageLoader(new Approximate(getImageManager()));
        }

        /**
         * Set the {@link ModelLoader} for the model.
         *
         * <p>
         *     Note - This method is required only if you are using a model for which there is no default
         *     {@link ModelLoader}. You can also optionally use this method to override the default {@link ModelLoader} for
         *     a model for which there is a default. The defaults are as follows:
         * <ul>
         *     <li>{@link String} - {@link StringLoader}. String must be a file path
         *          (<code>/data/data/com.bumptech/glide/...</code>), a url (<code>http://www.google.com</code>), or a
         *          uri. </li>
         *     <li>{@link File} - {@link FileLoader}</li>
         *     <li>{@link Integer} - {@link DrawableLoader}. Integer must be a resource id in your package</li>
         *     <li>{@link Uri} - {@link UriLoader}. Uri must be a scheme handled by
         *     {@link android.content.ContentResolver#openInputStream(android.net.Uri)}, http, or https</li>
         * </ul>
         * </p>
         *
         * <p>
         *     Note - If you have the ability to fetch different sized images for a given model, you should supply a
         *     {@link ModelLoader} here to do so. Fetching a smaller image means less bandwidth, battery, and memory
         *     usage as well as faster image loads. To simply build a url to download an image using the width and
         *     the height of the view, consider passing in a subclass of
         *     {@link com.bumptech.glide.loader.model.VolleyModelLoader}.
         * </p>
         *
         * @param modelLoader The {@link ModelLoader} to use. Replaces any existing loader
         * @return This Request
         */
        public Request<T> with(ModelLoader<T> modelLoader) {
            this.modelLoader = modelLoader;

            return this;
        }

        /**
         * Resize models using {@link ImageManager#centerCrop(String, com.bumptech.glide.loader.stream.StreamLoader, int, int, com.bumptech.glide.resize.LoadedCallback)}
         * Replaces any existing resize style
         *
         * @return This Request
         */
        public Request<T> centerCrop() {
            return resizeWith(new CenterCrop(getImageManager()));
        }

        /**
         * Resize models using {@link ImageManager#fitCenter(String, com.bumptech.glide.loader.stream.StreamLoader, int, int, com.bumptech.glide.resize.LoadedCallback)}
         * Replaces any existing resize style
         *
         * @return This Request
         */
        public Request<T> fitCenter() {
            return resizeWith(new FitCenter(getImageManager()));
        }

        /**
         * Resize models using {@link ImageManager#getImageApproximate(String, com.bumptech.glide.loader.stream.StreamLoader, int, int, com.bumptech.glide.resize.LoadedCallback)}
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
            builder.setImageLoader(imageLoader);

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
         * Sets a drawable to display while an image is loading
         *
         * @param drawable The drawable to use as a placeholder
         * @return This Request
         */
        public Request<T> setPlaceholderDrawable(Drawable drawable) {
            builder.setPlaceholderDrawable(drawable);
            return this;
        }

        /**
         * @see #setPlaceholderDrawable(android.graphics.drawable.Drawable)
         *
         * @param resourceId The id of the resource to use as a placeholder
         * @return This Request
         */
        public Request<T> setPlaceholderResource(int resourceId) {
            builder.setPlaceholderResource(resourceId);
            return this;
        }

        /**
         * Creates an {@link ImagePresenter} or retrieves the existing one and starts loading the image represented by
         * the given model. This must be called on the main thread.
         *
         * <p>
         *     Note - If an existing ImagePresenter already exists for this view it will not be replaced. This means you
         *     can set options once and only once the first time load and begin is called for any given view. For
         *     example, if you call load and begin for a view with centerCrop the first time and then load a second time
         *     for the same view but with fitCenter, the image will still be resized with centerCrop. If you need
         *     to change options you can call <code> imageView.setTag(R.id.image_presenter_id, null) </code> prior to
         *     calling this method, but it is inefficient to do so, particularly in lists.
         * </p>
         *
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
         */
        private void build() {
            if (presenter == null) {
                if (modelLoader == null) {
                    modelLoader = getModelFor(model, context);
                }
                presenter = builder.setModelLoader(modelLoader).build();
            }
        }
    }
}
