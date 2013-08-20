package com.bumptech.glide;

import android.content.Context;
import android.net.Uri;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import com.bumptech.glide.loader.image.ImageLoader;
import com.bumptech.glide.loader.image.ImageManagerLoader;
import com.bumptech.glide.loader.model.FileLoader;
import com.bumptech.glide.loader.model.GenericLoaderFactory;
import com.bumptech.glide.loader.model.ModelLoader;
import com.bumptech.glide.loader.model.ModelLoaderFactory;
import com.bumptech.glide.loader.model.ResourceLoader;
import com.bumptech.glide.loader.model.StringLoader;
import com.bumptech.glide.loader.model.UriLoader;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.loader.transformation.CenterCrop;
import com.bumptech.glide.loader.transformation.FitCenter;
import com.bumptech.glide.loader.transformation.MultiTransformationLoader;
import com.bumptech.glide.loader.transformation.None;
import com.bumptech.glide.loader.transformation.TransformationLoader;
import com.bumptech.glide.presenter.ImagePresenter;
import com.bumptech.glide.presenter.ImageReadyCallback;
import com.bumptech.glide.presenter.target.ImageViewTarget;
import com.bumptech.glide.presenter.target.Target;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.load.Downsampler;
import com.bumptech.glide.resize.load.Transformation;
import com.bumptech.glide.util.Log;
import com.bumptech.glide.volley.VolleyUrlLoader;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A singleton to present a simple static interface for Glide {@link Glide.Request} and to create and manage an
 * {@link ImageLoader} and {@link ModelLoaderFactory}s. This class provides most of the functionality of
 * {@link ImagePresenter} with a simpler but less efficient interface. For more complicated cases it may be worth
 * considering using {@link ImagePresenter} and {@link com.bumptech.glide.presenter.ImagePresenter.Builder} directly.
 *
 * <p>
 * Note - This class is not thread safe.
 * </p>
 */
public class Glide {
    private static final Glide GLIDE = new Glide();
    private final Map<Target, Metadata> metadataTracker = new WeakHashMap<Target, Metadata>();
    private final GenericLoaderFactory loaderFactory = new GenericLoaderFactory();
    private final WeakHashMap<ImageView, WeakReference<ImageViewTarget>> imageViewToTarget =
            new WeakHashMap<ImageView, WeakReference<ImageViewTarget>>();

    private ImageManager imageManager = null;

    /**
     * Get the singleton.
     *
     * @return the singleton
     */
    public static Glide get() {
        return GLIDE;
    }

    protected Glide() {
        loaderFactory.register(File.class, new FileLoader.Factory());
        loaderFactory.register(Integer.class, new ResourceLoader.Factory());
        loaderFactory.register(String.class, new StringLoader.Factory());
        loaderFactory.register(Uri.class, new UriLoader.Factory());
        try {
            Class.forName("com.bumptech.glide.volley.VolleyUrlLoader$Factory");
            loaderFactory.register(URL.class, new VolleyUrlLoader.Factory());
        } catch (ClassNotFoundException e) {
            Log.d("Volley not found, missing url loader");
            loaderFactory.register(URL.class, new ModelLoaderFactory<URL>() {
                ModelLoader<URL> errorUrlLoader = new ModelLoader<URL>() {
                    @Override
                    public StreamLoader getStreamLoader(URL model, int width, int height) {
                        throw new IllegalArgumentException("No ModelLoaderFactory for urls registered with Glide");
                    }

                    @Override
                    public String getId(URL model) {
                        throw new IllegalArgumentException("No ModelLoaderFactory for urls registered with Glide");
                    }
                };

                @Override
                public ModelLoader<URL> build(Context context, GenericLoaderFactory factories) {
                    return errorUrlLoader;
                }

                @Override @SuppressWarnings("unchecked")
                public Class<? extends ModelLoader<URL>> loaderClass() {
                    return (Class<ModelLoader<URL>>) errorUrlLoader.getClass();
                }

                @Override
                public void teardown() {
                }
            });
        }
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
     * Set the {@link ImageManager} to use with {@link Glide.Request}.
     *
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
     * Use the given factory to build a {@link ModelLoader} for models of the given class.
     *
     * <p>
     *     Note - If a factory already exists for the given class, it will be replaced. If that factory is not being
     *     used for any other model class, {@link com.bumptech.glide.loader.model.ModelLoaderFactory#teardown()}
     *     will be called.
     * </p>
     *
     * <p>
     *     Note - The factory must not be an anonymous inner class of an Activity or another object that cannot be
     *     retained statically.
     * </p>
     *
     * @param clazz The class
     * @param factory The factory to use
     * @param <T> The type of the model
     */
    public <T> void register(Class<T> clazz, ModelLoaderFactory<T> factory) {
        ModelLoaderFactory<T> removed = loaderFactory.register(clazz, factory);
        if (removed != null) {
            removed.teardown();
        }
    }

    /**
     * Build a {@link ModelLoader} for the given model class using a registered factory.
     *
     * @param clazz The class to get a {@link ModelLoader} for
     * @param context Any context
     * @param <T> The type of the model
     * @return A new {@link ModelLoader} for the given model class
     * @throws IllegalArgumentException if no factory exists for the given class
     */
    public static <T> ModelLoader<T> buildModelLoader(Class<T> clazz, Context context) {
        return GLIDE.loaderFactory.buildModelLoader(clazz, context);
    }

    @SuppressWarnings("unchecked")
    private <T> ModelLoaderFactory<T> getFactory(T model) {
        return loaderFactory.getFactory((Class<T>) model.getClass());
    }

    private ImageViewTarget getImageViewTarget(ImageView imageView) {
        final WeakReference<ImageViewTarget> ref = imageViewToTarget.get(imageView);
        return ref != null ? ref.get() : null;
    }

    private ImageViewTarget getImageViewTargetOrSet(ImageView imageView) {
        ImageViewTarget result = getImageViewTarget(imageView);
        if (result == null) {
            result = new ImageViewTarget(imageView);
            imageViewToTarget.put(imageView, new WeakReference<ImageViewTarget>(result));
        }
        return result;
    }

    /**
     * Set the {@link ModelLoaderFactory} and therefore the model type to use for a new load.
     *
     * <p>
     *     Note - You can use this method to set a {@link ModelLoaderFactory} for models that don't have a default
     *     {@link ModelLoader}/{@link ModelLoaderFactory}. You can also optionally use this method to override the
     *     default {@link ModelLoader} for a model for which there is a default. If you would like to permanently
     *     use this factory for all model loads of the this factory's type, see
     *     {@link #register(Class, com.bumptech.glide.loader.model.ModelLoaderFactory)}.
     * </p>
     *
     * <p>
     *     Note - If you have the ability to fetch different sized images for a given model, it is most efficient to
     *     supply a custom {@link ModelLoaderFactory} here to do so, even if a default exists. Fetching a smaller image
     *     means less bandwidth, battery, and memory usage as well as faster image loads. To simply build a url to
     *     download an image using the width and the height of the target, consider passing in a factory for a subclass
     *     of {@link com.bumptech.glide.loader.model.UrlModelLoader}
     * </p>
     *
     *
     * @param factory The {@link ModelLoaderFactory} to use to load an image from a given model
     * @param <T> The type of the model to load using this factory
     * @return A {@link ModelRequest} to set the specific model to load
     */
    public static <T> ModelRequest<T> using(ModelLoaderFactory<T> factory) {
        return new ModelRequest<T>(factory);
    }

    /**
     * Set the {@link ModelLoader} and therefore the model type to use for a new load.
     *
     * @see #using(com.bumptech.glide.loader.model.ModelLoaderFactory)
     *
     * @param modelLoader The model loader to use
     * @param <T> The type of the model to load using this loader
     * @return A {@link ModelRequest} to set the specific model to load
     */
    public static <T> ModelRequest<T> using(final ModelLoader<T> modelLoader) {
        return new ModelRequest<T>(new ModelLoaderFactory<T>() {
            @Override
            public ModelLoader<T> build(Context context, GenericLoaderFactory factories) {
                return modelLoader;
            }


            @Override @SuppressWarnings("unchecked")
            public Class<? extends ModelLoader<T>> loaderClass() {
                return (Class<ModelLoader<T>>) modelLoader.getClass();
            }

            @Override
            public void teardown() { }
        });
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link String} to load the image represented by the
     * given {@link String}. Defaults to {@link StringLoader.Factory} and {@link StringLoader} to load the given model.
     *
     * @see #using(ModelLoaderFactory)
     * @see ModelRequest#load(String)
     *
     * @param string The string representing the image. Must be either a path, or a uri handled by {@link UriLoader}
     * @return A {@link Request} to set options for the load and ultimately the target to load the model into
     */
    public static Request<String> load(String string) {
        return new Request<String>(string);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link Uri} to load the image at the given uri.
     * Defaults to {@link UriLoader.Factory} and {@link UriLoader}.
     *
     * @see #using(ModelLoaderFactory)
     * @see ModelRequest#load(android.net.Uri)
     *
     * @param uri The uri representing the image. Must be a uri handled by {@link UriLoader}
     * @return A {@link Request} to set options for the load and ultimately the target to load the model into
     */
    public static Request<Uri> load(Uri uri) {
        return new Request<Uri>(uri);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link URL} to load the image represented by the
     * given {@link URL}. Defaults to {@link VolleyUrlLoader.Factory} and {@link VolleyUrlLoader} to load the given
     * model.
     *
     * @see #using(ModelLoaderFactory)
     * @see ModelRequest#load(java.net.URL)
     *
     * @param url The URL representing the image.
     * @return A {@link Request} to set options for the load and ultimately the target to load the model into
     */
    public static Request<URL> load(URL url) {
        return new Request<URL>(url);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link File} to load the image represented by the
     * given {@link File}. Defaults to {@link FileLoader.Factory} and {@link FileLoader} to load the given model.
     *
     * @see #using(ModelLoaderFactory)
     * @see ModelRequest#load(java.io.File)
     *
     * @param file The File containing the image
     * @return A {@link Request} to set options for the load and ultimately the target to load the model into
     */
    public static Request<File> load(File file) {
        return new Request<File>(file);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link Integer} to load the image represented by the
     * given {@link Integer} resource id. Defaults to {@link ResourceLoader.Factory} and {@link ResourceLoader} to load
     * the given model.
     *
     * @see #using(ModelLoaderFactory)
     * @see ModelRequest#load(Integer)
     *
     * @param resourceId the id of the resource containing the image
     * @return A {@link Request} to set options for the load and ultimately the target to load the model into
     */
    public static Request<Integer> load(Integer resourceId) {
        return new Request<Integer>(resourceId);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for the given model type to load the image represented by
     * the given model.
     *
     * @param model The model to load
     * @param <T> The type of the model to load
     * @return A {@link Request} to set options for the load and ultimately the target to load the model into
     * @throws IllegalArgumentException If no {@link ModelLoaderFactory} is registered for the given model type
     */
    public static <T> Request<T> load(T model) {
        return new Request<T>(model);
    }

    /**
     * @see #cancel(com.bumptech.glide.presenter.target.Target)
     */
    public static boolean cancel(ImageView imageView) {
        final Target target = GLIDE.getImageViewTarget(imageView);
        return target != null && cancel(target);
    }

    /**
     * Cancel any pending loads Glide may have for the target. After the load is cancelled Glide will not load
     * a placeholder or bitmap into the target so it is safe to do so yourself until you start another load.
     *
     * @param target The Target to cancel loads for
     * @return True iff Glide had ever been asked to load an image for this target
     */
    public static boolean cancel(Target target) {
        ImagePresenter current = target.getImagePresenter();
        final boolean cancelled = current != null;
        if (cancelled) {
            current.clear();
        }

        return cancelled;
    }

    /**
     * A helper class for building requests with custom {@link ModelLoader}s
     *
     * @param <T> The type of the model (and {@link ModelLoader}
     */
    public static class ModelRequest<T> {
        private final ModelLoaderFactory<T> factory;

        private ModelRequest(ModelLoaderFactory<T> factory) {
            this.factory = factory;
        }

        public Request<T> load(T model) {
            return new Request<T>(model, factory);
        }
    }

    /**
     * Sets a variety of type independent options including resizing, animations, and placeholders. Responsible
     * for building or retrieving an ImagePresenter for the given target and passing the ImagePresenter the given model.
     *
     * @param <T> The type of model that will be loaded into the target
     */
    @SuppressWarnings("unused") //public api
    public static class Request<T> {

        private Context context;
        private Target target;

        private ModelLoaderFactory<T> modelLoaderFactory;
        private final T model;

        private int animationId = -1;
        private int placeholderId = -1;
        private int errorId = -1;
        private Downsampler downsampler = Downsampler.AT_LEAST;
        private ArrayList<TransformationLoader<T>> transformationLoaders = new ArrayList<TransformationLoader<T>>();

        private Request(T model) {
            this(model, GLIDE.getFactory(model));
        }

        private Request(T model, ModelLoaderFactory<T> factory) {
             if (model == null ) {
                throw new IllegalArgumentException("Model can't be null");
            }
            this.model = model;

            if (factory == null) {
                throw new IllegalArgumentException("No ModelLoaderFactory registered for class=" + model.getClass());
            }
            this.modelLoaderFactory = factory;
        }

        /**
         * Load images at a size near the size of the target using {@link Downsampler#AT_LEAST}.
         *
         * @see #downsample(com.bumptech.glide.resize.load.Downsampler)
         *
         * @return This Request
         */
        public Request<T> approximate() {
            return downsample(Downsampler.AT_LEAST);
        }

        /**
         * Load images at their original size using {@link Downsampler#NONE}.
         *
         * @see #downsample(com.bumptech.glide.resize.load.Downsampler)
         *
         * @return This Request
         */
        public Request<T> asIs() {
            return downsample(Downsampler.NONE);
        }

        /**
         * Load images using the given {@link Downsampler}. Replaces any existing downsampler. Defaults to
         * {@link Downsampler#AT_LEAST}
         *
         * @param downsampler The downsampler
         * @return This Request
         */
        public Request<T> downsample(Downsampler downsampler) {
            this.downsampler = downsampler;

            return this;
        }

        /**
         * Transform images using {@link com.bumptech.glide.loader.transformation.CenterCrop}.
         *
         * @see #transform(com.bumptech.glide.loader.transformation.TransformationLoader)
         *
         * @return This Request
         */
        public Request<T> centerCrop() {
            return transform(new CenterCrop<T>());
        }

        /**
         * Transform images using {@link com.bumptech.glide.loader.transformation.FitCenter}.
         *
         * @see #transform(com.bumptech.glide.loader.transformation.TransformationLoader)
         *
         * @return This Request
         */
        public Request<T> fitCenter() {
            return transform(new FitCenter<T>());
        }

        /**
         * Set an arbitrary transformation to apply after an image has been loaded into memory.
         *
         * @see #transform(com.bumptech.glide.loader.transformation.TransformationLoader)
         *
         * @param transformation The transformation to use
         * @return This Request
         */
        public Request<T> transform(final Transformation transformation) {
            return transform(new TransformationLoader<T>() {
                @Override
                public Transformation getTransformation(T model) {
                    return transformation;
                }

                @Override
                public String getId() {
                    return transformation.getId();
                }
            });
        }

        /**
         * Transform images with the given {@link TransformationLoader}. Appends this transformation onto any existing
         * transformations
         *
         * @param transformationLoader The loader to obtaian a transformation for a given model
         * @return This Request
         */
        public Request<T> transform(TransformationLoader<T> transformationLoader) {
            transformationLoaders.add(transformationLoader);

            return this;
        }

        /**
         * Sets an animation to run on the wrapped target when an image load finishes. Will only be run if the image
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
         * Start loading the image into the view.
         *
         * <p>
         *     Note - This method will call {@link ImageView#setTag(Object)} and may silently overwrite any tag that
         *     might already be set on the view.
         * </p>
         *
         * @see ImagePresenter#setModel(Object)
         * @param imageView The view that will display the image
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

            finish(imageView.getContext(), GLIDE.getImageViewTargetOrSet(imageView));
        }

        /**
         * Set the target the image will be loaded into.
         *
         * <p>
         *     Note - This method does not actually start loading the view. You must first pass in a {@link Context} to
         *     returned Request via {@link ContextRequest#with(android.content.Context)}.
         * </p>
         *
         * @param target The target to load te image for
         * @return A {@link ContextRequest} that can start the load
         */
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
         * Creates the new {@link ImagePresenter} if one does not currently exist for the current target and sets it as
         * the target's ImagePresenter via {@link Target#setImagePresenter(com.bumptech.glide.presenter.ImagePresenter)}
         */
        @SuppressWarnings("unchecked")
        private ImagePresenter<T> getImagePresenter(Target target) {
            Metadata previous = GLIDE.metadataTracker.get(target);
            Metadata current = new Metadata(this);

            ImagePresenter<T> result = target.getImagePresenter();

            if (!current.isIdenticalTo(previous)) {
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
            TransformationLoader<T> transformationLoader = getFinalTransformationLoader();

            ImagePresenter.Builder<T> builder = new ImagePresenter.Builder<T>()
                    .setTarget(target, context)
                    .setModelLoader(modelLoaderFactory.build(context, GLIDE.loaderFactory))
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

        private TransformationLoader<T> getFinalTransformationLoader() {
            switch (transformationLoaders.size()) {
                case 0:
                    return new None<T>();
                case 1:
                    return transformationLoaders.get(0);
                default:
                    return new MultiTransformationLoader<T>(transformationLoaders);
            }
        }

        private String getFinalTransformationId() {
            switch (transformationLoaders.size()) {
                case 0:
                    return Transformation.NONE.getId();
                case 1:
                    return transformationLoaders.get(0).getId();
                default:
                    StringBuilder sb = new StringBuilder();
                    for (TransformationLoader transformationLoader : transformationLoaders) {
                        sb.append(transformationLoader.getId());
                    }
                    return sb.toString();
            }
        }
    }

    /**
     * An request for the user to provide an {@link Context} to start an image load
     */
    public static class ContextRequest {
        private final Request request;
        private final Target target;

        private ContextRequest(Request request, Target target) {
            this.request = request;
            this.target = target;
        }

        /**
         * Start loading the image using the given context. The context will not be referenced statically so any
         * context is acceptable.
         *
         * @param context The context to use to help load the image
         */
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
            modelLoaderClass = request.modelLoaderFactory.loaderClass();
            downsamplerId = request.downsampler.getId();
            transformationId = request.getFinalTransformationId();
            animationId = request.animationId;
            placeholderId = request.placeholderId;
            errorId = request.errorId;
        }

        //we don't want to change behavior in sets/maps, just be able to compare properties
        public boolean isIdenticalTo(Metadata metadata) {
            if (metadata == null) return false;
            if (animationId != metadata.animationId) return false;
            if (errorId != metadata.errorId) return false;
            if (placeholderId != metadata.placeholderId) return false;
            if (!downsamplerId.equals(metadata.downsamplerId)) return false;
            if (!modelClass.equals(metadata.modelClass)) return false;
            if (!modelLoaderClass.equals(metadata.modelLoaderClass)) return false;
            if (!transformationId.equals(metadata.transformationId)) return false;

            return true;
        }
    }
}
