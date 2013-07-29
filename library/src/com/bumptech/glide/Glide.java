package com.bumptech.glide;

import android.content.Context;
import android.net.Uri;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.loader.image.ImageLoader;
import com.bumptech.glide.loader.model.FileLoader;
import com.bumptech.glide.loader.model.ModelLoader;
import com.bumptech.glide.loader.model.ResourceLoader;
import com.bumptech.glide.loader.model.StringLoader;
import com.bumptech.glide.loader.model.UriLoader;
import com.bumptech.glide.loader.model.UrlLoader;
import com.bumptech.glide.presenter.ImagePresenter;
import com.bumptech.glide.presenter.ImageReadyCallback;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.loader.Approximate;
import com.bumptech.glide.resize.loader.AsIs;
import com.bumptech.glide.resize.loader.CenterCrop;
import com.bumptech.glide.resize.loader.FitCenter;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import static android.view.ViewGroup.LayoutParams;

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
     * for use with {@link Glide.Request} and/or as an easily accessible singleton
     *
     * @return true iff a {@link RequestQueue} has already been set
     */
    public boolean isRequestQueueSet() {
        return requestQueue != null;
    }

    /**
     * Set the {@link RequestQueue} to use with {@link Glide.Request}. Replaces the current
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
     * {@link ImageManager} with custom options for use with {@link com.bumptech.glide.Glide.Request} and/or as an
     * easily accessible singleton.
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
     * Set the {@link ImageManager} to use with {@link Glide.Request} Replaces the current
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
     * Set the {@link ModelLoader} for this view.
     *
     * <p>
     *     Note - You can use this method to set a {@link ModelLoader} for models that don't have a default
     *     {@link ModelLoader}. You can also optionally use this method to override the default {@link ModelLoader}
     *     for a model for which there is a default.
     * </p>
     *
     * <p>
     *     Note - If you have the ability to fetch different sized images for a given model, it is most efficient to
     *     supply a custom {@link ModelLoader} here to do so, even if a default exists. Fetching a smaller image
     *     means less bandwidth, battery, and memory usage as well as faster image loads. To simply build a url to
     *     download an image using the width and the height of the view, consider passing in a subclass of
     *     {@link com.bumptech.glide.loader.model.VolleyModelLoader}.
     * </p>
     *
     * @param modelLoader The {@link ModelRequest} to use to load an image from a given model
     * @return A {@link ModelRequest} to set the specific model to load
     */
    public static <T> ModelRequest<T> using(ModelLoader<T> modelLoader) {
        return new ModelRequest<T>(modelLoader);
    }

    /**
     * Use {@link StringLoader} to load the given model
     *
     * @see #using(com.bumptech.glide.loader.model.ModelLoader)
     *
     * @param string The string representing the image. Must be either a path, or a uri handled by {@link UriLoader}
     * @return A {@link Request} to set options for the load and ultimately the view to load the model into
     */
    public static Request<String> load(String string) {
        return new Request<String>(string);
    }

    /**
     * Use {@link UriLoader} to load the given model
     *
     * @see #using(com.bumptech.glide.loader.model.ModelLoader)
     *
     * @param uri The uri representing the image. Must be a uri handled by {@link UriLoader}
     * @return A {@link Request} to set options for the load and ultimately the view to load the model into
     */
    public static Request<Uri> load(Uri uri) {
        return new Request<Uri>(uri);
    }

    /**
     * Use {@link UrlLoader} to load the given model
     *
     * @see #using(com.bumptech.glide.loader.model.ModelLoader)
     *
     * @param url The URL representing the image.
     * @return A {@link Request} to set options for the load and ultimately the view to load the model into
     */
    public static Request<URL> load(URL url) {
        return new Request<URL>(url);
    }

    /**
     * Use {@link FileLoader} to load the given model
     *
     * @see #using(com.bumptech.glide.loader.model.ModelLoader)
     *
     * @param file The File containing the image
     * @return A {@link Request} to set options for the load and ultimately the view to load the model into
     */
    public static Request<File> load(File file) {
        return new Request<File>(file);
    }

    /**
     * Use {@link com.bumptech.glide.loader.model.ResourceLoader} to load the given model
     *
     * @see #using(com.bumptech.glide.loader.model.ModelLoader)
     *
     * @param resourceId the id of the resource containing the image
     * @return A {@link Request} to set options for the load and ultimately the view to load the model into
     */
    public static Request<Integer> load(Integer resourceId) {
        return new Request<Integer>(resourceId);
    }

    private interface ModelLoaderFactory<T> {
        public ModelLoader<T> build(Context context);
        public Class<? extends ModelLoader<T>> loaderClass();
    }

    private static final ModelLoaderFactory<String> stringLoaderFactory = new ModelLoaderFactory<String>() {

        @Override
        public ModelLoader<String> build(Context context) {
            return new StringLoader(context);
        }

        @Override
        public Class<? extends ModelLoader<String>> loaderClass() {
            return StringLoader.class;
        }
    };

    private static final ModelLoaderFactory<Uri> uriLoaderFactory = new ModelLoaderFactory<Uri>() {
        @Override
        public ModelLoader<Uri> build(Context context) {
            return new UriLoader(context);
        }

        @Override
        public Class<? extends ModelLoader<Uri>> loaderClass() {
            return UriLoader.class;
        }
    };

    private static final ModelLoaderFactory<File> fileLoaderFactory = new ModelLoaderFactory<File>() {
        @Override
        public ModelLoader<File> build(Context context) {
            return new FileLoader(context);
        }

        @Override
        public Class<? extends ModelLoader<File>> loaderClass() {
            return FileLoader.class;
        }
    };

    private static final ModelLoaderFactory<URL> urlLoaderFactory = new ModelLoaderFactory<URL>() {
        @Override
        public ModelLoader<URL> build(Context context) {
            return new UrlLoader(context);
        }

        @Override
        public Class<? extends ModelLoader<URL>> loaderClass() {
            return UrlLoader.class;
        }
    };

    private static final ModelLoaderFactory<Integer> resourceLoaderFactory = new ModelLoaderFactory<Integer>() {
        @Override
        public ModelLoader<Integer> build(Context context) {
            return new ResourceLoader(context);
        }

        @Override
        public Class<? extends ModelLoader<Integer>> loaderClass() {
            return ResourceLoader.class;
        }
    };

    @SuppressWarnings("unchecked")
    private static <T> ModelLoaderFactory<T> getFactory(T model) {
        final ModelLoaderFactory result;
        if (model instanceof String) {
            result = stringLoaderFactory;
        } else if (model instanceof Uri) {
            result = uriLoaderFactory;
        } else if (model instanceof URL) {
            result = urlLoaderFactory;
        } else if (model instanceof File) {
            result = fileLoaderFactory;
        } else if (model instanceof Integer) {
            result = resourceLoaderFactory;
        } else {
            throw new IllegalArgumentException("No model factory for model class=" + model.getClass());
        }
        return result;
    }

    /**
     * A helper class for building requests with custom {@link ModelLoader}s
     *
     * @param <T> The type of the model (and {@link ModelLoader}
     */
    public static class ModelRequest<T> {
        private final ModelLoader<T> modelLoader;

        public ModelRequest(ModelLoader<T> modelLoader) {
            this.modelLoader = modelLoader;
        }

        public Request<T> load(T model) {
            return new Request<T>(model, modelLoader);
        }
    }

    /**
     * Sets a variety of type independent options including resizing, animations, and placeholders. Responsible
     * for building or retrieving an ImagePresenter for the given view and passing the ImagePresenter the given model.
     *
     * @param <T> The type of model that will be loaded into the view
     */
    @SuppressWarnings("unused") //public api
    public static class Request<T> {

        private enum ResizeOption {
            APPROXIMATE,
            CENTER_CROP,
            FIT_CENTER,
            AS_IS,
        }

        private ModelLoaderFactory<T> modelLoaderFactory;
        private final T model;
        private final Class<? extends ModelLoader> modelLoaderClass;
        private ModelLoader<T> modelLoader;

        private int animationId = -1;
        private int placeholderId = -1;
        private int errorId = -1;
        private ResizeOption resizeOption = null;
        private ImageLoader imageLoader = null;

        public Request(T model) {
            this.model = model;
            this.modelLoaderFactory = getFactory(model);
            this.modelLoaderClass = modelLoaderFactory.loaderClass();
        }

        public Request(T model, ModelLoader<T> modelLoader) {
            this.model = model;
            this.modelLoader = modelLoader;
            this.modelLoaderClass = modelLoader.getClass();
        }

        /**
         * Resize models using {@link CenterCrop}. Replaces any existing resize style
         *
         * @return This Request
         */
        public Request<T> centerCrop() {
            resizeOption = ResizeOption.CENTER_CROP;
            imageLoader = null;

            return this;
        }

        /**
         * Resize models using {@link FitCenter}. Replaces any existing resize style
         *
         * @return This Request
         */
        public Request<T> fitCenter() {
            resizeOption = ResizeOption.FIT_CENTER;
            imageLoader = null;

            return this;
        }

        /**
         * Resize models using {@link Approximate}. Replaces any existing resize style
         *
         * @return This Request
         */
        public Request<T> approximate() {
            resizeOption = ResizeOption.APPROXIMATE;
            imageLoader = null;

            return this;
        }

        /**
         * Load images at their original size using {@link com.bumptech.glide.resize.loader.AsIs}. Replaces any existing
         * resize style
         *
         * @return This Request
         */
        public Request<T> asIs() {
            resizeOption = ResizeOption.AS_IS;
            imageLoader = null;

            return this;
        }

        /**
         * Set the {@link ImageLoader} to use to load images into memory
         *
         * @param imageLoader The {@link ImageLoader} to use
         * @return This Request
         */
        public Request<T> resizeWith(ImageLoader imageLoader) {
            this.imageLoader = imageLoader;
            resizeOption = null;

            return this;
        }

        /**
         * Sets an animation to run on the wrapped view when an image load finishes. Will only be run if the image
         * was loaded asynchronously (ie was not in the memory cache)
         *
         * @param animationId The resource id of the animation to run
         * @return This Request
         */
        public Request<T> animate(int animationId) {
            this.animationId = animationId;

            return this;
        }

        /**
         * Sets a resource to display while an image is loading
         *
         * @param resourceId The id of the resource to use as a placeholder
         * @return This Request
         */
        public Request<T> placeholder(int resourceId) {
            this.placeholderId = resourceId;

            return this;
        }

        /**
         * Sets a resource to display if a load fails
         *
         * @param resourceId The id of the resource to use as a placeholder
         * @return This request
         */
        public Request<T> error(int resourceId) {
            this.errorId = resourceId;

            return this;
        }

        /**
         * Creates an {@link ImagePresenter} or retrieves the existing one and starts loading the image represented by
         * the given model. This must be called on the main thread.
         *
         * @see ImagePresenter#setModel(Object)
         */
        public void into(ImageView imageView) {
            ImagePresenter<T> imagePresenter = getImagePresenter(imageView);
            imagePresenter.setModel(model);
        }

        /**
         * Creates the new {@link ImagePresenter} if one does not currently exist for the current view and sets it as
         * the view's tag for the id {@code R.id.image_presenter_id}.
         */
        private ImagePresenter<T> getImagePresenter(ImageView imageView) {
            resizeOption = getFinalResizeOption(imageView);

            Metadata previous = getMetadataFrom(imageView);
            Metadata current = new Metadata(this);

            ImagePresenter<T> result = ImagePresenter.getCurrent(imageView);

            if (!current.equals(previous)) {
                if (result != null) {
                    result.clear();
                }

                result = buildImagePresenter(imageView);

                setMetadata(imageView, current);
            }

            return result;
        }

        private ImagePresenter<T> buildImagePresenter(ImageView imageView) {
            final Context context = imageView.getContext();

            imageLoader = getFinalImageLoader(context);
            modelLoader = getFinalModelLoader(context);

            ImagePresenter.Builder<T> builder = new ImagePresenter.Builder<T>()
                    .setImageView(imageView)
                    .setModelLoader(modelLoader)
                    .setImageLoader(imageLoader);

            if (animationId != -1) {
                final Animation animation = AnimationUtils.loadAnimation(imageView.getContext(), animationId);
                builder.setImageReadyCallback(new ImageReadyCallback() {
                    @Override
                    public void onImageReady(ImageView view, boolean fromCache) {
                        view.clearAnimation();

                        if (!fromCache) {
                            view.startAnimation(animation);
                        }

                    }
                });
            }

            if (placeholderId != -1) {
                builder.setPlaceholderResource(placeholderId);
            }


            if (errorId != -1) {
                builder.setErrorResource(errorId);
            }

            return builder.build();
        }

        private ModelLoader<T> getFinalModelLoader(Context context) {
            if (modelLoader == null) {
                return modelLoaderFactory.build(context);
            } else {
                return modelLoader;
            }
        }

        private ImageLoader getFinalImageLoader(Context context) {
            if (imageLoader == null) {
                return getImageLoaderFromOptions(context);
            } else {
                return imageLoader;
            }
        }

        private ResizeOption getFinalResizeOption(ImageView imageView) {
            ResizeOption result = resizeOption;
            if (result == null) {
                //default to Approximate unless view's layout params are set to wrap content, in which case the only
                //loader that makes sense is AsIs since all the others crop based on the view's size
                final LayoutParams lp = imageView.getLayoutParams();
                if (lp != null && (lp.width == LayoutParams.WRAP_CONTENT || lp.height == LayoutParams.WRAP_CONTENT)) {
                    result = ResizeOption.AS_IS;
                } else {
                    result = ResizeOption.APPROXIMATE;
                }
            }
            return result;
        }

        private ImageLoader getImageLoaderFromOptions(Context context) {

            Class<? extends ImageLoader> imageLoaderClass = getImageLoaderClassFor(resizeOption);
            try {
                return imageLoaderClass.getConstructor(Context.class).newInstance(context);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            return null;
        }

        private static Class<? extends ImageLoader> getImageLoaderClassFor(ResizeOption resizeOption) {
            final Class<? extends ImageLoader> imageLoaderClass;
            switch (resizeOption) {
                case APPROXIMATE:
                    imageLoaderClass = Approximate.class;
                    break;
                case CENTER_CROP:
                    imageLoaderClass = CenterCrop.class;
                    break;
                case FIT_CENTER:
                    imageLoaderClass = FitCenter.class;
                    break;
                case AS_IS:
                    imageLoaderClass = AsIs.class;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown resize option " + resizeOption);
            }
            return imageLoaderClass;
        }

        private static Metadata getMetadataFrom(ImageView imageView) {
            return (Metadata) imageView.getTag(R.id.glide_metadata);
        }

        private static void setMetadata(ImageView imageView, Metadata metadata) {
            imageView.setTag(R.id.glide_metadata, metadata);
        }

        private static class Metadata {
            public final Class modelClass;
            public final Class modelLoaderClass;
            public final Class imageLoaderClass;
            public final int animationId;
            public final int placeholderId;
            public final int errorId;

            public Metadata(Request request) {
                modelClass = request.model.getClass();
                modelLoaderClass = request.modelLoaderClass;
                if (request.imageLoader != null) {
                    imageLoaderClass = request.imageLoader.getClass();
                } else {
                    imageLoaderClass = getImageLoaderClassFor(request.resizeOption);

                }
                animationId = request.animationId;
                placeholderId = request.placeholderId;
                errorId = request.errorId;
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || !(o instanceof Metadata)) {
                    return false;
                }

                Metadata other = (Metadata) o;

                return modelClass.equals(other.modelClass) &&
                        modelLoaderClass.equals(other.modelLoaderClass) &&
                        imageLoaderClass.equals(other.imageLoaderClass) &&
                        animationId == other.animationId &&
                        placeholderId == other.placeholderId &&
                        errorId == other.errorId;

            }
        }

    }
}
