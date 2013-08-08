package com.bumptech.glide;

import android.content.Context;
import android.net.Uri;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.loader.image.ImageLoader;
import com.bumptech.glide.loader.image.ImageManagerLoader;
import com.bumptech.glide.loader.model.FileLoader;
import com.bumptech.glide.loader.model.ModelLoader;
import com.bumptech.glide.loader.model.ResourceLoader;
import com.bumptech.glide.loader.model.StringLoader;
import com.bumptech.glide.loader.model.UriLoader;
import com.bumptech.glide.loader.model.UrlLoader;
import com.bumptech.glide.loader.transformation.CenterCrop;
import com.bumptech.glide.loader.transformation.FitCenter;
import com.bumptech.glide.loader.transformation.TransformationLoader;
import com.bumptech.glide.presenter.ImagePresenter;
import com.bumptech.glide.presenter.ImageReadyCallback;
import com.bumptech.glide.presenter.target.ImageViewTarget;
import com.bumptech.glide.presenter.target.Target;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.load.Downsampler;
import com.bumptech.glide.resize.load.Transformation;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.WeakHashMap;

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
    private final Map<Target, Metadata> metadataTracker = new WeakHashMap<Target, Metadata>();

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

    /**
     * @see #cancel(com.bumptech.glide.presenter.target.Target)
     */
    public static boolean cancel(ImageView imageView) {
        return cancel(new ImageViewTarget(imageView));
    }

    /**
     * Cancel any pending loads Glide may have for the target. After the load is cancelled Glide will not load
     * a placeholder or bitmap into the target so it is safe to do so yourself until you start another load.
     *
     * @param target The Target to cancel loads for
     * @return True iff Glide had ever been asked to load an image for this view
     */
    public static boolean cancel(Target target) {
        ImagePresenter current = target.getImagePresenter();
        final boolean cancelled = current != null;
        if (cancelled) {
            current.clear();
        }

        return cancelled;
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

        private ModelRequest(ModelLoader<T> modelLoader) {
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

        private Context context;
        private Target target;

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
        private Transformation transformation = Transformation.NONE;
        private Downsampler downsampler = Downsampler.AT_LEAST;
        private TransformationLoader<T> transformationLoader = null;

        private Request(T model) {
            if (model == null ) {
                throw new IllegalArgumentException("Model can't be null");
            }
            this.model = model;
            this.modelLoaderFactory = getFactory(model);
            if (modelLoaderFactory == null ) {
                throw new IllegalArgumentException("Missing ModelLoader factory for model = " + model);
            }
            this.modelLoaderClass = modelLoaderFactory.loaderClass();
        }

        private Request(T model, ModelLoader<T> modelLoader) {
            if (model == null ) {
                throw new IllegalArgumentException("Model can't be null");
            }
            if (modelLoader == null) {
                throw new IllegalArgumentException("ModelLoader can't be null");
            }
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
            transformation = Transformation.CENTER_CROP;
            downsampler = Downsampler.AT_LEAST;
            transformationLoader = null;

            return this;
        }

        /**
         * Resize models using {@link FitCenter}. Replaces any existing resize style
         *
         * @return This Request
         */
        public Request<T> fitCenter() {
            transformation = Transformation.FIT_CENTER;
            downsampler = Downsampler.AT_LEAST;
            transformationLoader = null;

            return this;
        }

        /**
         * Load images at a size near the size of the target using {@link Downsampler#AT_LEAST}. Replaces any existing resize style
         *
         * @return This Request
         */
        public Request<T> approximate() {
            transformation = Transformation.NONE;
            downsampler = Downsampler.AT_LEAST;
            transformationLoader = null;

            return this;
        }

        /**
         * Load images at their original size using {@link Downsampler#NONE}. Replaces any existing
         * resize style
         *
         * @return This Request
         */
        public Request<T> asIs() {
            transformation = Transformation.NONE;
            downsampler = Downsampler.NONE;
            transformationLoader = null;

            return this;
        }

        /**
         * Set an arbitrary transformation to apply after an image has been loaded into memory.  Replaces any existing
         * resize style
         *
         * @param transformation The transformation to use
         * @return This Request
         */
        public Request<T> transform(final Transformation transformation) {
            this.transformation = transformation;
            downsampler = Downsampler.AT_LEAST;
            transformationLoader = null;

            return this;
        }

        public Request<T> transform(TransformationLoader<T> transformationLoader) {
            this.transformationLoader = transformationLoader;
            transformation = null;
            downsampler = Downsampler.AT_LEAST;

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
            //make an effort to support wrap content layout params. This will still blow
            //up if transformation doesn't handle wrap content, but its a start
            final ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
            if (layoutParams != null &&
                    (layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT ||
                    layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT)) {
                downsampler = Downsampler.NONE;
            }

            finish(imageView.getContext(), new ImageViewTarget(imageView));
        }

        public ContextRequest into(Target target) {
            return new ContextRequest(this, target);
        }

        private void finish(Context context, Target target) {
            this.context = context;
            this.target = target;

            ImagePresenter<T> imagePresenter = getImagePresenter(target);
            imagePresenter.setModel(model);
        }

        /**
         * Creates the new {@link ImagePresenter} if one does not currently exist for the current view and sets it as
         * the view's tag for the id {@code R.id.image_presenter_id}.
         */
        @SuppressWarnings("unchecked")
        private ImagePresenter<T> getImagePresenter(Target target) {
            Metadata previous = GLIDE.metadataTracker.get(target);
            Metadata current = new Metadata(this);

            ImagePresenter<T> result = target.getImagePresenter();

            if (!current.equals(previous)) {
                if (result != null) {
                    result.clear();
                }

                result = buildImagePresenter(target);
                target.setImagePresenter(result);

                GLIDE.metadataTracker.put(target, current);
            }

            return result;
        }

        private ImagePresenter<T> buildImagePresenter(Target target) {
            modelLoader = getFinalModelLoader(context);
            transformationLoader = getFinalTransformationLoader();

            ImagePresenter.Builder<T> builder = new ImagePresenter.Builder<T>()
                    .setTarget(target, context)
                    .setModelLoader(modelLoader)
                    .setImageLoader(new ImageManagerLoader(context, downsampler))
                    .setTransformationLoader(transformationLoader);

            if (animationId != -1) {
                final Animation animation = AnimationUtils.loadAnimation(context, animationId);
                builder.setImageReadyCallback(new ImageReadyCallback() {
                    @Override
                    public void onImageReady(Target target, boolean fromCache) {
                        if (!fromCache) {
                            target.startAnimation(animation);
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

        private TransformationLoader<T> getFinalTransformationLoader() {
            if (transformationLoader != null) {
                return transformationLoader;
            } else {
                return new TransformationLoader<T>() {
                    @Override
                    public Transformation getTransformation(T model) {
                        return transformation;
                    }
                };
            }
        }

        private String getFinalTransformationId() {
            if (transformationLoader != null) {
                return transformationLoader.getClass().toString();
            } else {
                return transformation.getId();
            }
        }
    }

    public static class ContextRequest {
        private final Request request;
        private final Target target;

        private ContextRequest(Request request, Target target) {
            this.request = request;
            this.target = target;
        }

        public void with(Context context) {
            request.finish(context, target);
        }
    }

    private static class Metadata {
        public final Class modelClass;
        public final Class modelLoaderClass;
        public final int animationId;
        public final int placeholderId;
        public final int errorId;

        private final String downsamplerId;
        private final String transformationId;

        public Metadata(Request request) {
            modelClass = request.model.getClass();
            modelLoaderClass = request.modelLoaderClass;
            downsamplerId = request.downsampler.getId();
            transformationId = request.getFinalTransformationId();
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
                    downsamplerId.equals(other.downsamplerId) &&
                    transformationId.equals(other.transformationId) &&
                    animationId == other.animationId &&
                    placeholderId == other.placeholderId &&
                    errorId == other.errorId;

        }
    }
}
