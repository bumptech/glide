package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import com.bumptech.glide.loader.bitmap.BaseBitmapLoadFactory;
import com.bumptech.glide.loader.bitmap.model.file_descriptor.FileDescriptorFileLoader;
import com.bumptech.glide.loader.bitmap.model.file_descriptor.FileDescriptorModelLoader;
import com.bumptech.glide.loader.bitmap.model.file_descriptor.FileDescriptorResourceLoader;
import com.bumptech.glide.loader.bitmap.model.file_descriptor.FileDescriptorStringLoader;
import com.bumptech.glide.loader.bitmap.model.file_descriptor.FileDescriptorUriLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamResourceLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamFileLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamModelLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamStringLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamUriLoader;
import com.bumptech.glide.loader.image.ImageLoader;
import com.bumptech.glide.loader.image.ImageManagerLoader;
import com.bumptech.glide.loader.bitmap.model.GenericLoaderFactory;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.model.ModelLoaderFactory;
import com.bumptech.glide.loader.bitmap.transformation.CenterCrop;
import com.bumptech.glide.loader.bitmap.transformation.FitCenter;
import com.bumptech.glide.loader.bitmap.transformation.MultiTransformationLoader;
import com.bumptech.glide.loader.bitmap.transformation.None;
import com.bumptech.glide.loader.bitmap.transformation.TransformationLoader;
import com.bumptech.glide.presenter.ImagePresenter;
import com.bumptech.glide.presenter.target.ImageViewTarget;
import com.bumptech.glide.presenter.target.Target;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.load.BitmapDecoder;
import com.bumptech.glide.resize.load.Downsampler;
import com.bumptech.glide.resize.load.Transformation;
import com.bumptech.glide.resize.load.VideoBitmapDecoder;
import com.bumptech.glide.volley.VolleyUrlLoader;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A singleton to present a simple static interface for Glide {@link GenericRequest} and to create and manage an
 * {@link ImageLoader} and {@link ModelLoaderFactory}s. This class provides most of the functionality of
 * {@link ImagePresenter} with a simpler but less efficient interface. For more complicated cases it may be worth
 * considering using {@link ImagePresenter} and {@link com.bumptech.glide.presenter.ImagePresenter.Builder} directly.
 *
 * <p>
 * Note - This class is not thread safe.
 * </p>
 */
public class Glide {
    private static final String TAG = "Glide";
    private static final Glide GLIDE = new Glide();
    private final Map<Target, Metadata> metadataTracker = new WeakHashMap<Target, Metadata>();
    private final GenericLoaderFactory loaderFactory = new GenericLoaderFactory();

    private ImageManager imageManager = null;

    /**
     * A class for monitoring the status of a request while images load.
     *
     * @param <T> The type of the model being loaded
     */
    public interface RequestListener<T> {

        /**
         * Called when an exception occurs during a load. Will only be called if we currently want to display an image
         * for the given model in the given target. It is recommended to create a single instance per activity/fragment
         * rather than instantiate a new object for each call to {@code Glide.load()} to avoid object churn.
         *
         * <p>
         *     It is safe to reload this or a different model or change what is displayed in the target at this point.
         *     For example:
         * <pre>
         * <code>
         *     public void onException(Exception e, ModelType model, Target target) {
         *         target.setPlaceholder(R.drawable.a_specific_error_for_my_exception);
         *         Glide.load(model).into(target);
         *     }
         * </code>
         * </pre>
         * </p>
         *
         * <p>
         *     Note - if you want to reload this or any other model after an exception, you will need to include all
         *     relevant builder calls (like centerCrop, placeholder etc).
         * </p>
         *
         * @param e The exception, or null
         * @param model The model we were trying to load when the exception occurred
         * @param target The {@link Target} we were trying to load the image into
         */
        public abstract void onException(Exception e, T model, Target target);

        /**
         * Called when a load completes successfully, immediately after
         * {@link Target#onImageReady(android.graphics.Bitmap)}.
         *
         * @param model The specific model that was used to load the image.
         * @param target The target the model was loaded into.
         */
        public abstract void onImageReady(T model, Target target);
    }

    /**
     * Get the singleton.
     *
     * @return the singleton
     */
    public static Glide get() {
        return GLIDE;
    }

    protected Glide() {
        register(File.class, ParcelFileDescriptor.class, new FileDescriptorFileLoader.Factory());
        register(File.class, InputStream.class, new StreamFileLoader.Factory());
        register(Integer.class, ParcelFileDescriptor.class, new FileDescriptorResourceLoader.Factory());
        register(Integer.class, InputStream.class, new StreamResourceLoader.Factory());
        register(String.class, ParcelFileDescriptor.class, new FileDescriptorStringLoader.Factory());
        register(String.class, InputStream.class, new StreamStringLoader.Factory());
        register(Uri.class, ParcelFileDescriptor.class, new FileDescriptorUriLoader.Factory());
        register(Uri.class, InputStream.class, new StreamUriLoader.Factory());
        try {
            Class.forName("com.bumptech.glide.volley.VolleyUrlLoader$Factory");
            register(URL.class, InputStream.class, new VolleyUrlLoader.Factory());
        } catch (ClassNotFoundException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Volley not found, missing url loader");
            }
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
     * {@link ImageManager} with custom options for use with {@link GenericRequest} and/or as an
     * easily accessible singleton.
     *
     * @return true iff an {@link ImageManager} is currently set
     */
    public boolean isImageManagerSet() {
        return imageManager != null;
    }

    /**
     * Set the {@link ImageManager} to use with {@link GenericRequest}.
     *
     * @see #setImageManager(com.bumptech.glide.resize.ImageManager)
     *
     * @param builder The builder that will be used to construct a new ImageManager
     */
    public void setImageManager(ImageManager.Builder builder) {
        setImageManager(builder.build());
    }

    /**
     * Set the {@link ImageManager} to use with {@link GenericRequest} Replaces the current
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
     * Use the given factory to build a {@link ModelLoader} for models of the given class. Generally the best use of
     * this method is to replace one of the default factories or add an implementation for other similar low level
     * models. Typically the {@link Glide#using(StreamModelLoader)} or {@link Glide#using(FileDescriptorModelLoader)}
     * syntax is preferred because it directly links the model with the ModelLoader being used to load it.
     *
     * <p>
     *     Note - If a factory already exists for the given class, it will be replaced. If that factory is not being
     *     used for any other model class, {@link ModelLoaderFactory#teardown()}
     *     will be called.
     * </p>
     *
     * <p>
     *     Note - The factory must not be an anonymous inner class of an Activity or another object that cannot be
     *     retained statically.
     * </p>
     *
     * @see #using(FileDescriptorModelLoader)
     * @see #using(StreamModelLoader)
     *
     * @param modelClass The model class.
     * @param resourceClass The resource class the model loader will translate the model type into.
     * @param factory The factory to use.
     * @param <T> The type of the model.
     * @param <Y> the type of the resource.
     */
    public <T, Y> void register(Class<T> modelClass, Class<Y> resourceClass, ModelLoaderFactory<T, Y> factory) {
        ModelLoaderFactory<T, Y> removed = loaderFactory.register(modelClass, resourceClass, factory);
        if (removed != null) {
            removed.teardown();
        }
    }

    /**
     * Build a {@link ModelLoader} for the given model class using a registered factory.
     *
     * @param modelClass The class to get a {@link ModelLoader} for
     * @param context Any context
     * @param <T> The type of the model
     * @return A new {@link ModelLoader} for the given model class
     * @throws IllegalArgumentException if no factory exists for the given class
     */
    public static <T, Y> ModelLoader<T, Y> buildModelLoader(Class<T> modelClass, Class<Y> resourceClass, Context context) {
        return GLIDE.loaderFactory.buildModelLoader(modelClass, resourceClass, context);
    }

    @SuppressWarnings("unchecked")
    private <T, Y> ModelLoaderFactory<T, Y> getFactory(T model, Class<Y> resourceClass) {
        return loaderFactory.getFactory((Class<T>) model.getClass(), resourceClass);
    }

    private ImageViewTarget getImageViewTarget(ImageView imageView) {
        Object tag = imageView.getTag();
        ImageViewTarget result = null;
        if (tag instanceof ImageViewTarget) {
            result = (ImageViewTarget) tag;
        } else if (tag != null) {
            throw new IllegalArgumentException("You cannot set a tag on an image view Glide is loading an image into");
        }

        return result;
    }

    private ImageViewTarget getImageViewTargetOrSet(ImageView imageView) {
        ImageViewTarget result = getImageViewTarget(imageView);
        if (result == null) {
            result = new ImageViewTarget(imageView);
            imageView.setTag(result);
        }
        return result;
    }

    /**
     * Set the {@link ModelLoader} to use for for a new load where the model loader translates from a model to an
     * {@link InputStream} resource for loading images.
     *
     * @param modelLoader The model loader to use.
     * @param <T> The type of the model.
     * @return A new {@link ImageModelRequest}.
     */
    public static <T> ImageModelRequest<T> using(final StreamModelLoader<T> modelLoader) {
        return new ImageModelRequest<T>(modelLoaderToFactory(modelLoader));
    }

    /**
     * Set the {@link ModelLoader} to use for a new load where the model loader translates from a model to an
     * {@link ParcelFileDescriptor} resource for loading video thumbnails.
     *
     * @param modelLoader The model loader to use.
     * @param <T> The type of the model.
     * @return A new {@link VideoModelRequest}.
     */
    public static <T> VideoModelRequest<T> using(final FileDescriptorModelLoader<T> modelLoader) {
        return new VideoModelRequest<T>(modelLoaderToFactory(modelLoader));

    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link String} to load the image represented by the
     * given {@link String}. Defaults to {@link StreamStringLoader.Factory} and {@link StreamStringLoader} to load the given model.
     *
     * @see #using(StreamModelLoader)
     *
     * @param string The string representing the image. Must be either a path, or a uri handled by {@link StreamUriLoader}
     * @return A {@link GenericRequest} to set options for the load and ultimately the target to load the model into
     */
    public static Request<String> load(String string) {
        return new Request<String>(string);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link Uri} to load the image at the given uri.
     * Defaults to {@link StreamUriLoader.Factory} and {@link StreamUriLoader}.
     *
     * @see #using(StreamModelLoader)
     *
     * @param uri The uri representing the image. Must be a uri handled by {@link StreamUriLoader}
     * @return A {@link GenericRequest} to set options for the load and ultimately the target to load the model into
     */
    public static Request<Uri> load(Uri uri) {
        return new Request<Uri>(uri);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link URL} to load the image represented by the
     * given {@link URL}. Defaults to {@link VolleyUrlLoader.Factory} and {@link VolleyUrlLoader} to load the given
     * model.
     *
     * @see #using(StreamModelLoader)
     *
     * @param url The URL representing the image.
     * @return A {@link GenericRequest} to set options for the load and ultimately the target to load the model into
     */
    public static Request<URL> load(URL url) {
        return new Request<URL>(url);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link File} to load the image represented by the
     * given {@link File}. Defaults to {@link StreamFileLoader.Factory} and {@link StreamFileLoader} to load the given model.
     *
     * @see #using(StreamModelLoader)
     *
     * @param file The File containing the image
     * @return A {@link GenericRequest} to set options for the load and ultimately the target to load the model into
     */
    public static Request<File> load(File file) {
        return new Request<File>(file);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link Integer} to load the image represented by the
     * given {@link Integer} resource id. Defaults to {@link StreamResourceLoader.Factory} and {@link StreamResourceLoader} to load
     * the given model.
     *
     * @see #using(StreamModelLoader)
     *
     * @param resourceId the id of the resource containing the image
     * @return A {@link GenericRequest} to set options for the load and ultimately the target to load the model into
     */
    public static Request<Integer> load(Integer resourceId) {
        return new Request<Integer>(resourceId);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for the given model type to load the image represented by
     * the given model.
     *
     * @param model The model to load.
     * @param <T> The type of the model to load.
     * @return A {@link GenericRequest} to set options for the load and ultimately the target to load the image into.
     * @throws IllegalArgumentException If no such {@link ModelLoaderFactory} is registered for the given model type.
     */
    @SuppressWarnings("unused")
    public static <T> Request<T> loadFromImage(T model) {
        return new ImageModelRequest<T>(GLIDE.getFactory(model, InputStream.class)).load(model);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for the given model type for
     * {@link ParcelFileDescriptor}s to load a thumbnail for the video represented by the given model.
     *
     * @param model The model to load.
     * @param <T> The type of the model to load.
     * @return A {@link Request} to set options for the load an ultimately the target to load the thumbnail into.
     * @throws IllegalArgumentException If no such {@link ModelLoaderFactory} is registered for the given model type.
     */
    @SuppressWarnings("unused")
    public static <T> Request<T> loadFromVideo(T model) {
        return new VideoModelRequest<T>(GLIDE.getFactory(model, ParcelFileDescriptor.class)).loadFromVideo(model);
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
     * A helper class for building requests with custom {@link ModelLoader}s that translate models to
     * {@link ParcelFileDescriptor} resources for loading video thumbnails.
     *
     * @param <T> The type of the model.
     */
    public static class VideoModelRequest<T> {
        private ModelLoaderFactory<T, ParcelFileDescriptor> factory;

        private VideoModelRequest(ModelLoaderFactory<T, ParcelFileDescriptor> factory) {
            this.factory = factory;
        }

        public Request<T> loadFromVideo(T model) {
            return new Request<T>(model, null, factory);
        }
    }

    /**
     * A helper class for building requests with custom {@link ModelLoader}s that translate models to
     * {@link InputStream} resources for loading images.
     *
     * @param <T> The type of the model.
     */
    public static class ImageModelRequest<T> {
        private final ModelLoaderFactory<T, InputStream> factory;

        private ImageModelRequest(ModelLoaderFactory<T, InputStream> factory) {
            this.factory = factory;
        }

        public Request<T> load(T model) {
            return new Request<T>(model, factory, null);
        }
    }

    /**
     * An request for the user to provide an {@link Context} to start an image load.
     */
    public static class ContextRequest {
        private final GenericRequest request;
        private final Target target;

        private ContextRequest(GenericRequest request, Target target) {
            this.request = request;
            this.target = target;
        }

        /**
         * Start loading the image using the given context. The context will not be referenced statically so any
         * context is acceptable.
         *
         * @param context The context to use to help load the image.
         */
        public void with(Context context) {
            request.finish(context, target);
        }
    }

     /**
     * A class for creating a request to load a bitmap for an image or from a video. Sets a variety of type independent
      * options including resizing, animations, and placeholders.
     *
     * @param <ModelType> The type of model that will be loaded into the target.
     */
    @SuppressWarnings("unused") //public api
    public static class Request<ModelType> extends GenericRequest<ModelType, InputStream, ParcelFileDescriptor> {
        private Request(ModelType model) {
            this(model, GLIDE.getFactory(model, InputStream.class),
                    GLIDE.getFactory(model, ParcelFileDescriptor.class));
        }

        private Request(ModelType model,
                ModelLoaderFactory<ModelType, InputStream> imageFactory,
                ModelLoaderFactory<ModelType, ParcelFileDescriptor> videoFactory) {
            super(model, imageFactory, videoFactory);
            approximate().videoDecoder(new VideoBitmapDecoder());
        }

        /**
         * Load images at a size near the size of the target using {@link Downsampler#AT_LEAST}.
         *
         * @see #downsample(com.bumptech.glide.resize.load.Downsampler)
         *
         * @return This Request
         */
        public Request<ModelType> approximate() {
            return downsample(Downsampler.AT_LEAST);
        }

        /**
         * Load images at their original size using {@link Downsampler#NONE}.
         *
         * @see #downsample(com.bumptech.glide.resize.load.Downsampler)
         *
         * @return This Request
         */
        public Request<ModelType> asIs() {
            return downsample(Downsampler.NONE);
        }

        /**
         * Load images using the given {@link Downsampler}. Replaces any existing image decoder. Defaults to
         * {@link Downsampler#AT_LEAST}. Will be ignored if the data represented by the model is a video.
         *
         * @see #imageDecoder
         * @see #videoDecoder(BitmapDecoder)
         *
         * @param downsampler The downsampler
         * @return This Request
         */
        public Request<ModelType> downsample(Downsampler downsampler) {
            super.imageDecoder(downsampler);
            return this;
        }

        @Override
        public Request<ModelType> imageDecoder(BitmapDecoder<InputStream> decoder) {
            super.imageDecoder(decoder);
            return this;
        }

        @Override
        public Request<ModelType> videoDecoder(BitmapDecoder<ParcelFileDescriptor> decoder) {
            super.videoDecoder(decoder);
            return this;
        }

        @Override
        public Request<ModelType> centerCrop() {
            super.centerCrop();
            return this;
        }

        @Override
        public Request<ModelType> fitCenter() {
            super.fitCenter();
            return this;
        }

        @Override
        public Request<ModelType> transform(Transformation transformation) {
            super.transform(transformation);
            return this;
        }

        @Override
        public Request<ModelType> transform(
                TransformationLoader<ModelType> transformationLoader) {
            super.transform(transformationLoader);
            return this;
        }

        @Override
        public Request<ModelType> animate(int animationId) {
            super.animate(animationId);
            return this;
        }

        @Override
        public Request<ModelType> placeholder(int resourceId) {
            super.placeholder(resourceId);
            return this;
        }

        @Override
        public Request<ModelType> error(int resourceId) {
            super.error(resourceId);
            return this;
        }

        @Override
        public Request<ModelType> listener(RequestListener<ModelType> requestListener) {
            super.listener(requestListener);
            return this;
        }
    }

    /**
     * A generic class that can handle loading a bitmap either from an image or as a thumbnail from a video given
     * models loaders to translate a model into generic resources for either an image or a video and decoders that can
     * decode those resources into bitmaps.
     *
     * @param <ModelType> The type of model representing the image or video.
     * @param <ImageResourceType> The resource type that the image {@link ModelLoader} will provide that can be decoded
     *                           by the image {@link BitmapDecoder}.
     * @param <VideoResourceType> The resource type that the video {@link ModelLoader} will provide that can be decoded
     *                           by the video {@link BitmapDecoder}.
     */
    private static class GenericRequest<ModelType, ImageResourceType, VideoResourceType> {
        private Context context;
        private ModelLoaderFactory<ModelType, ImageResourceType> imageModelLoaderFactory;
        private final ModelLoaderFactory<ModelType, VideoResourceType> videoModelLoaderFactory;
        private final ModelType model;

        private int animationId = -1;
        private int placeholderId = -1;
        private int errorId = -1;
        private ArrayList<TransformationLoader<ModelType>> transformationLoaders = new ArrayList<TransformationLoader<ModelType>>();
        private RequestListener<ModelType> requestListener;
        private BitmapDecoder<ImageResourceType> imageDecoder;
        private BitmapDecoder<VideoResourceType> videoDecoder;

        private GenericRequest(ModelType model, ModelLoaderFactory<ModelType, ImageResourceType> imageFactory,
                ModelLoaderFactory<ModelType, VideoResourceType> videoFactory) {
             if (model == null ) {
                throw new IllegalArgumentException("Model can't be null");
            }
            this.model = model;

            if (imageFactory == null && videoFactory == null) {
                throw new IllegalArgumentException("No ModelLoaderFactorys registered for either image or video type,"
                        + " class=" + model.getClass());
            }
            this.imageModelLoaderFactory = imageFactory;
            this.videoModelLoaderFactory = videoFactory;
        }

        /**
         * Loads the image from the given resource type into an {@link Bitmap} using the given {@link BitmapDecoder}.
         *
         * <p>
         *     Will be ignored if the data represented by the given model is not an image.
         * </p>
         *
         * @see Downsampler
         *
         * @param decoder The {@link BitmapDecoder} to use to decode the image resource.
         * @return This Request.
         */
        public GenericRequest<ModelType, ImageResourceType, VideoResourceType> imageDecoder(
                BitmapDecoder<ImageResourceType> decoder) {
            this.imageDecoder = decoder;

            return this;
        }

        /**
         * Loads the video from the given resource type into an {@link Bitmap} using the given {@link BitmapDecoder}.
         *
         * <p>
         *     Will be ignored if the data represented by the given model is not a video.
         * </p>
         *
         * @see VideoBitmapDecoder
         *
         * @param decoder The {@link BitmapDecoder} to use to decode the video resource.
         * @return This request.
         */
        public GenericRequest<ModelType, ImageResourceType, VideoResourceType> videoDecoder(
                BitmapDecoder<VideoResourceType> decoder) {
            this.videoDecoder = decoder;

            return this;
        }

        /**
         * Transform images using {@link CenterCrop}.
         *
         * @see #transform(TransformationLoader)
         *
         * @return This Request
         */
        public GenericRequest<ModelType, ImageResourceType, VideoResourceType> centerCrop() {
            return transform(new CenterCrop<ModelType>());
        }

        /**
         * Transform images using {@link FitCenter}.
         *
         * @see #transform(TransformationLoader)
         *
         * @return This Request
         */
        public GenericRequest<ModelType, ImageResourceType, VideoResourceType> fitCenter() {
            return transform(new FitCenter<ModelType>());
        }

        /**
         * Set an arbitrary transformation to apply after an image has been loaded into memory.
         *
         * @see #transform(TransformationLoader)
         *
         * @param transformation The transformation to use
         * @return This Request
         */
        public GenericRequest<ModelType, ImageResourceType, VideoResourceType> transform(
                final Transformation transformation) {
            return transform(new TransformationLoader<ModelType>() {
                @Override
                public Transformation getTransformation(ModelType model) {
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
        public GenericRequest<ModelType, ImageResourceType, VideoResourceType> transform(
                TransformationLoader<ModelType> transformationLoader) {
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
        public GenericRequest<ModelType, ImageResourceType, VideoResourceType> animate(int animationId) {
            this.animationId = animationId;

            return this;
        }

        /**
         * Sets a resource to display while an image is loading
         *
         * @param resourceId The id of the resource to use as a placeholder
         * @return This Request
         */
        public GenericRequest<ModelType, ImageResourceType, VideoResourceType> placeholder(int resourceId) {
            this.placeholderId = resourceId;

            return this;
        }

        /**
         * Sets a resource to display if a load fails
         *
         * @param resourceId The id of the resource to use as a placeholder
         * @return This request
         */
        public GenericRequest<ModelType, ImageResourceType, VideoResourceType> error(int resourceId) {
            this.errorId = resourceId;

            return this;
        }

        /**
         * Sets a Request listener to monitor the image load. It's best to create a single instance of an exception
         * handler per type of request (usually activity/fragment) rather than pass one in per request to avoid some
         * redundant object allocation.
         *
         * @param requestListener The request listener to use
         * @return This request
         */
        public GenericRequest<ModelType, ImageResourceType, VideoResourceType> listener(
                RequestListener<ModelType> requestListener) {
            this.requestListener = requestListener;

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

            ImagePresenter<ModelType> imagePresenter = getImagePresenter(target);
            imagePresenter.setModel(model);
        }

        /**
         * Creates the new {@link ImagePresenter} if one does not currently exist for the current target and sets it as
         * the target's ImagePresenter via {@link Target#setImagePresenter(com.bumptech.glide.presenter.ImagePresenter)}
         */
        @SuppressWarnings("unchecked")
        private ImagePresenter<ModelType> getImagePresenter(Target target) {
            ImagePresenter<ModelType> result = target.getImagePresenter();

            Metadata previous = GLIDE.metadataTracker.get(target);
            Metadata current = new Metadata(this);

            if (previous != null && result == null) {
                previous = null;
            }

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

        private ImagePresenter<ModelType> buildImagePresenter(final Target target) {
            TransformationLoader<ModelType> transformationLoader = getFinalTransformationLoader();

            ModelLoader<ModelType, ImageResourceType> imageModelLoader = null;
            if (imageModelLoaderFactory != null) {
                imageModelLoader = imageModelLoaderFactory.build(context, GLIDE.loaderFactory);
            }
            ModelLoader<ModelType, VideoResourceType> videoModelLoader = null;
            if (videoModelLoaderFactory != null) {
                videoModelLoader = videoModelLoaderFactory.build(context, GLIDE.loaderFactory);
            }

            ImagePresenter.Builder<ModelType> builder = new ImagePresenter.Builder<ModelType>()
                    .setTarget(target, context)
                    .setBitmapLoadFactory(new BaseBitmapLoadFactory<ModelType, ImageResourceType, VideoResourceType>(
                            imageModelLoader, imageDecoder, videoModelLoader, videoDecoder, transformationLoader))
                    .setImageLoader(new ImageManagerLoader(context));

            if (animationId != -1 || requestListener != null) {
                final Animation animation;
                if (animationId != -1) {
                    animation = AnimationUtils.loadAnimation(context, animationId);
                } else {
                    animation = null;
                }
                builder.setImageReadyCallback(new ImagePresenter.ImageReadyCallback<ModelType>() {
                    @Override
                    public void onImageReady(ModelType model, Target target, boolean fromCache) {
                        if (animation != null && !fromCache) {
                            target.startAnimation(animation);
                        }
                        if (requestListener != null) {
                            requestListener.onImageReady(null, target);
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

            if (requestListener != null) {
                builder.setExceptionHandler(new ImagePresenter.ExceptionHandler<ModelType>() {
                    @Override
                    public void onException(Exception e, ModelType model, boolean isCurrent) {
                        if (isCurrent) {
                            requestListener.onException(e, model, target);
                        }
                    }
                });
            }

            return builder.build();
        }

        private TransformationLoader<ModelType> getFinalTransformationLoader() {
            switch (transformationLoaders.size()) {
                case 0:
                    return new None<ModelType>();
                case 1:
                    return transformationLoaders.get(0);
                default:
                    return new MultiTransformationLoader<ModelType>(transformationLoaders);
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


    private static <T, Y> ModelLoaderFactory<T, Y> modelLoaderToFactory(final ModelLoader<T, Y> modelLoader) {
        return new ModelLoaderFactory<T, Y>() {
            @Override
            public ModelLoader<T, Y> build(Context context, GenericLoaderFactory factories) {
                return modelLoader;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Class<? extends ModelLoader<T, Y>> loaderClass() {
                return (Class<ModelLoader<T, Y>>) modelLoader.getClass();
            }

            @Override
            public void teardown() { }
        };
    }

    private static class Metadata {
        public final Class modelClass;
        public final Class modelLoaderClass;
        public final int animationId;
        public final int placeholderId;
        public final int errorId;

        private final String transformationId;
        private final Class requestListenerClass;
        private final String decoderId;

        public Metadata(GenericRequest request) {
            modelClass = request.model.getClass();
            modelLoaderClass = request.imageModelLoaderFactory
                    .loaderClass();
            decoderId = request.imageDecoder
                    .getId();
            transformationId = request.getFinalTransformationId();
            animationId = request.animationId;
            placeholderId = request.placeholderId;
            errorId = request.errorId;
            requestListenerClass = (request.requestListener != null ?
                    request.requestListener.getClass() : null);
        }

        //we don't want to change behavior in sets/maps, just be able to compare properties
        public boolean isIdenticalTo(Metadata metadata) {
            if (metadata == null) return false;
            if (animationId != metadata.animationId) return false;
            if (errorId != metadata.errorId) return false;
            if (placeholderId != metadata.placeholderId) return false;
            if (!decoderId.equals(metadata.decoderId)) return false;
            if (!modelClass.equals(metadata.modelClass)) return false;
            if (!modelLoaderClass.equals(metadata.modelLoaderClass)) return false;
            if (!transformationId.equals(metadata.transformationId)) return false;
            if (requestListenerClass == null ? metadata.requestListenerClass != null :
                    !requestListenerClass.equals(metadata.requestListenerClass)) return false;

            return true;
        }
    }
}
