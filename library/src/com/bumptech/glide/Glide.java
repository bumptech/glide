package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.ImageView;
import com.android.volley.RequestQueue;
import com.bumptech.glide.loader.bitmap.ImageVideoBitmapLoadFactory;
import com.bumptech.glide.loader.bitmap.ResourceBitmapLoadFactory;
import com.bumptech.glide.loader.bitmap.model.GenericLoaderFactory;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.model.ModelLoaderFactory;
import com.bumptech.glide.loader.bitmap.model.file_descriptor.FileDescriptorFileLoader;
import com.bumptech.glide.loader.bitmap.model.file_descriptor.FileDescriptorModelLoader;
import com.bumptech.glide.loader.bitmap.model.file_descriptor.FileDescriptorResourceLoader;
import com.bumptech.glide.loader.bitmap.model.file_descriptor.FileDescriptorStringLoader;
import com.bumptech.glide.loader.bitmap.model.file_descriptor.FileDescriptorUriLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamByteArrayLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamFileLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamModelLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamResourceLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamStringLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamUriLoader;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.Priority;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.resize.cache.DiskCache;
import com.bumptech.glide.resize.cache.MemoryCache;
import com.bumptech.glide.resize.load.BitmapDecoder;
import com.bumptech.glide.resize.load.Downsampler;
import com.bumptech.glide.resize.load.MultiTransformation;
import com.bumptech.glide.resize.load.Transformation;
import com.bumptech.glide.resize.load.VideoBitmapDecoder;
import com.bumptech.glide.resize.request.BitmapRequestBuilder;
import com.bumptech.glide.resize.request.Request;
import com.bumptech.glide.resize.request.ThumbnailRequestCoordinator;
import com.bumptech.glide.resize.target.ImageViewTarget;
import com.bumptech.glide.resize.target.Target;
import com.bumptech.glide.resize.target.ViewTarget;
import com.bumptech.glide.volley.VolleyUrlLoader;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A singleton to present a simple static interface for building requests with {@link RequestBuilder} and maintaining
 * an {@link ImageManager} and it's {@link BitmapPool}, {@link DiskCache} and {@link MemoryCache}.
 *
 * <p>
 * Note - This class is not thread safe.
 * </p>
 */
public class Glide {
    private static Glide GLIDE;
    private final GenericLoaderFactory loaderFactory = new GenericLoaderFactory();
    private final RequestQueue requestQueue;
    private final ImageManager imageManager;

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
        public abstract void onImageReady(T model, Target target, boolean isFromMemoryCache, boolean isAnyImageSet);
    }

    /**
     * Get the singleton.
     *
     * @return the singleton
     */
    public static Glide get(Context context) {
        if (GLIDE == null) {
            GLIDE = new GlideBuilder(context).createGlide();
        }

        return GLIDE;
    }

    /**
     * Returns false if the {@link Glide} singleton has not yet been created and can therefore be setup using
     * {@link #setup(GlideBuilder)}.
     *
     * @see #setup(GlideBuilder)
     */
    public static boolean isSetup() {
        return GLIDE != null;
    }

    /**
     * Creates the {@link Glide} singleton using the given builder. Can be used to set options like cache sizes and
     * locations.
     *
     * @see #isSetup()
     *
     * @param builder The builder.
     * @throws IllegalArgumentException if the Glide singleton has already been created.
     */
    public static void setup(GlideBuilder builder) {
        if (GLIDE != null) {
            throw new IllegalArgumentException("Glide is already setup, check with isSetup() first");
        }

        GLIDE = builder.createGlide();
    }

    Glide(ImageManager imageManager, RequestQueue requestQueue) {
        this.imageManager = imageManager;
        this.requestQueue = requestQueue;
        register(File.class, ParcelFileDescriptor.class, new FileDescriptorFileLoader.Factory());
        register(File.class, InputStream.class, new StreamFileLoader.Factory());
        register(Integer.class, ParcelFileDescriptor.class, new FileDescriptorResourceLoader.Factory());
        register(Integer.class, InputStream.class, new StreamResourceLoader.Factory());
        register(String.class, ParcelFileDescriptor.class, new FileDescriptorStringLoader.Factory());
        register(String.class, InputStream.class, new StreamStringLoader.Factory());
        register(Uri.class, ParcelFileDescriptor.class, new FileDescriptorUriLoader.Factory());
        register(Uri.class, InputStream.class, new StreamUriLoader.Factory());
        register(URL.class, InputStream.class, new VolleyUrlLoader.Factory(requestQueue));
    }

    /**
     * Returns the {@link ImageManager} Glide is using to load images.
     */
    public ImageManager getImageManager() {
        return imageManager;
    }

    /**
     * Returns the {@link RequestQueue} Glide is using to fetch images over http/https.
     */
    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    /**
     * Use the given factory to build a {@link ModelLoader} for models of the given class. Generally the best use of
     * this method is to replace one of the default factories or add an implementation for other similar low level
     * models. Typically the {@link ModelRequest#using(StreamModelLoader)} or
     * {@link ModelRequest#using(FileDescriptorModelLoader)} syntax is preferred because it directly links the model
     * with the ModelLoader being used to load it.
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
     * @see ModelRequest#using(FileDescriptorModelLoader)
     * @see ModelRequest#using(StreamModelLoader)
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

    @SuppressWarnings("unchecked")
    private <T, Y> ModelLoaderFactory<T, Y> getFactory(T model, Class<Y> resourceClass) {
        return loaderFactory.getFactory((Class<T>) model.getClass(), resourceClass);
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
    public static <T, Y> ModelLoader<T, Y> buildModelLoader(Class<T> modelClass, Class<Y> resourceClass,
            Context context) {
        return Glide.get(context).loaderFactory.buildModelLoader(modelClass, resourceClass, context);
    }

    /**
     * A convenience method to build a {@link ModelLoader} for the given model that produces {@link InputStream}s using
     * a registered factory.
     *
     * @see #buildModelLoader(Class, Class, android.content.Context)
     */
    public static <T> ModelLoader<T, InputStream> buildStreamModelLoader(Class<T> modelClass, Context context) {
        return buildModelLoader(modelClass, InputStream.class, context);
    }

    /**
     * A convenience method to build a {@link ModelLoader} for the given model class that produces
     * {@link ParcelFileDescriptor}s using a registered factory.
     *
     * @see #buildModelLoader(Class, Class, android.content.Context)
     */
    public static <T> ModelLoader<T, ParcelFileDescriptor> buildFileDescriptorModelLoader(Class<T> modelClass,
            Context context) {
        return buildModelLoader(modelClass, ParcelFileDescriptor.class, context);
    }

    /**
     * Cancel any pending loads Glide may have for the target and free any resources (such as {@link Bitmap}s) that may
     * have been loaded for the target so they may be reused.
     *
     * @param target The Target to cancel loads for.
     */
    public static void clear(Target target) {
        Request request = target.getRequest();
        if (request!= null) {
            request.clear();
        }
    }

    /**
     * Cancel any pending loads Glide may have for the view and free any resources that may have been loaded for the
     * view.
     *
     * <p>
     *     Note that this will only work if {@link View#setTag(Object)} is not called on this view outside of Glide.
     * </p>
     *
     * @see #clear(Target).
     *
     * @param view The view to cancel loads and free resources for.
     * @throws IllegalArgumentException if an object other than Glide's metadata is set as the view's tag.
     */
    public static void clear(View view) {
        Target viewTarget = new CancelTarget(view);
        clear(viewTarget);
    }

    /**
     * Begin a load with Glide by passing in a context.
     *
     * @param context Any context, will not be retained.
     * @return A model request to pass in the object representing the image to be loaded.
     */
    public static ModelRequest with(Context context) {
        return new ModelRequest(context);
    }

    /**
     * A {@link RequestBuilder} builder that returns a request for a model that represents an image.
     */
    public static class ModelRequest {
        private final Context context;

        private ModelRequest(Context context) {
            this.context = context;
        }

        /**
         * Set the {@link ModelLoader} to use for for a new load where the model loader translates from a model to an
         * {@link InputStream} resource for loading images.
         *
         * @param modelLoader The model loader to use.
         * @param <T> The type of the model.
         * @return A new {@link ImageModelRequest}.
         */
        public <T> ImageModelRequest<T> using(final StreamModelLoader<T> modelLoader) {
            return new ImageModelRequest<T>(context, modelLoaderToFactory(modelLoader));
        }

        /**
         * A convenience method to use a {@link StreamByteArrayLoader} to decode an image from a byte array.
         *
         * @param modelLoader The byte array loader.
         * @return A new {@link ImageModelRequest}.
         */
        public ImageModelRequest<byte[]> using(StreamByteArrayLoader modelLoader) {
            return new ImageModelRequest<byte[]>(context, modelLoaderToFactory(modelLoader));
        }

        /**
         * Set the {@link ModelLoader} to use for a new load where the model loader translates from a model to an
         * {@link ParcelFileDescriptor} resource for loading video thumbnails.
         *
         * @param modelLoader The model loader to use.
         * @param <T> The type of the model.
         * @return A new {@link VideoModelRequest}.
         */
        public <T> VideoModelRequest<T> using(final FileDescriptorModelLoader<T> modelLoader) {
            return new VideoModelRequest<T>(context, modelLoaderToFactory(modelLoader));
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for {@link String} to load the image represented by
         * the given {@link String}. Defaults to {@link StreamStringLoader.Factory} and {@link StreamStringLoader} to
         * load the given model.
         *
         * @see #using(StreamModelLoader)
         *
         * @param string The string representing the image. Must be either a path, or a uri handled by
         *      {@link StreamUriLoader}
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the model into
         */
        public RequestBuilder<String> load(String string) {
            return new RequestBuilder<String>(context, string);
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for {@link Uri} to load the image at the given uri.
         * Defaults to {@link StreamUriLoader.Factory} and {@link StreamUriLoader}.
         *
         * @see #using(StreamModelLoader)
         *
         * @param uri The uri representing the image. Must be a uri handled by {@link StreamUriLoader}
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the model into
         */
        public RequestBuilder<Uri> load(Uri uri) {
            return new RequestBuilder<Uri>(context, uri);
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for {@link File} to load the image represented by the
         * given {@link File}. Defaults to {@link StreamFileLoader.Factory} and {@link StreamFileLoader} to load the given model.
         *
         * @see #using(StreamModelLoader)
         *
         * @param file The File containing the image
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the model into
         */
        public RequestBuilder<File> load(File file) {
            return new RequestBuilder<File>(context, file);
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for {@link Integer} to load the image represented by the
         * given {@link Integer} resource id. Defaults to {@link StreamResourceLoader.Factory} and {@link StreamResourceLoader} to load
         * the given model.
         *
         * @see #using(StreamModelLoader)
         *
         * @param resourceId the id of the resource containing the image
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the model into
         */
        public RequestBuilder<Integer> load(Integer resourceId) {
            return new RequestBuilder<Integer>(context, resourceId);
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for the given model type to load the image represented by
         * the given model.
         *
         * @param model The model to load.
         * @param <T> The type of the model to load.
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the image into.
         * @throws IllegalArgumentException If no such {@link ModelLoaderFactory} is registered for the given model type.
         */
        @SuppressWarnings("unused")
        public <T> RequestBuilder<T> loadFromImage(T model) {
            return new ImageModelRequest<T>(context, Glide.get(context).getFactory(model, InputStream.class))
                    .load(model);
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for {@link URL} to load the image represented by the
         * given {@link URL}. Defaults to {@link VolleyUrlLoader.Factory} and {@link VolleyUrlLoader} to load the given
         * model.
         *
         * @see #using(StreamModelLoader)
         *
         * @param url The URL representing the image.
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the model into
         */
        public RequestBuilder<URL> loadFromImage(URL url) {
            return new ImageModelRequest<URL>(context, Glide.get(context).getFactory(url, InputStream.class)).load(url);
        }

        /**
         * Use a new {@link StreamByteArrayLoader} to load an image from the given model.
         *
         * @see #loadFromImage(byte[])
         *
         * @param model The data to load.
         * @param id A unique id that identifies the image represented by the model suitable for use as a cache key
         *           (url, filepath etc). If there is no suitable id, use {@link #loadFromImage(byte[])} instaed.
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the image into.
         */
        public RequestBuilder<byte[]> loadFromImage(byte[] model, final String id) {
            return new ImageModelRequest<byte[]>(context, modelLoaderToFactory(new StreamByteArrayLoader() {
                @Override
                public String getId(byte[] model) {
                    return id;
                }
            })).load(model);
        }

        /**
         * Use a new {@link StreamByteArrayLoader} to load an image from the given model. Suitable when there is no
         * simple id that represents the given data.
         *
         * @param model the data to load.
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the image into.
         */
        public RequestBuilder<byte[]> loadFromImage(byte[] model) {
            return loadFromImage(model, UUID.randomUUID()
                    .toString());
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for the given model type for
         * {@link ParcelFileDescriptor}s to load a thumbnail for the video represented by the given model.
         *
         * @param model The model to load.
         * @param <T> The type of the model to load.
         * @return A {@link RequestBuilder} to set options for the load an ultimately the target to load the thumbnail into.
         * @throws IllegalArgumentException If no such {@link ModelLoaderFactory} is registered for the given model type.
         */
        @SuppressWarnings("unused")
        public <T> RequestBuilder<T> loadFromVideo(T model) {
            return new VideoModelRequest<T>(context, Glide.get(context).getFactory(model, ParcelFileDescriptor.class))
                    .loadFromVideo(model);
        }
    }

    /**
     * A helper class for building requests with custom {@link ModelLoader}s that translate models to
     * {@link ParcelFileDescriptor} resources for loading video thumbnails.
     *
     * @param <T> The type of the model.
     */
    public static class VideoModelRequest<T> {
        private final Context context;
        private ModelLoaderFactory<T, ParcelFileDescriptor> factory;

        private VideoModelRequest(Context context, ModelLoaderFactory<T, ParcelFileDescriptor> factory) {
            this.context = context;
            this.factory = factory;
        }

        public RequestBuilder<T> loadFromVideo(T model) {
            return new RequestBuilder<T>(context, model, null, factory);
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
        private final Context context;

        private ImageModelRequest(Context context, ModelLoaderFactory<T, InputStream> factory) {
            this.context = context;
            this.factory = factory;
        }

        public RequestBuilder<T> load(T model) {
            return new RequestBuilder<T>(context, model, factory, null);
        }
    }

    /**
     * A class for creating a request to load a bitmap for an image or from a video. Sets a variety of type independent
     * options including resizing, animations, and placeholders.
     *
     * @param <ModelType> The type of model that will be loaded into the target.
     */
    @SuppressWarnings("unused") //public api
    public static class RequestBuilder<ModelType> extends
            GenericRequestBuilder<ModelType, InputStream, ParcelFileDescriptor> {
        private RequestBuilder(Context context, ModelType model) {
            this(context, model, Glide.get(context).getFactory(model, InputStream.class),
                    Glide.get(context).getFactory(model, ParcelFileDescriptor.class));
        }

        private RequestBuilder(Context context, ModelType model,
                ModelLoaderFactory<ModelType, InputStream> imageFactory,
                ModelLoaderFactory<ModelType, ParcelFileDescriptor> videoFactory) {
            super(context, model, imageFactory, videoFactory);
            approximate().videoDecoder(new VideoBitmapDecoder());
        }

        /**
         * Load images at a size near the size of the target using {@link Downsampler#AT_LEAST}.
         *
         * @see #downsample(com.bumptech.glide.resize.load.Downsampler)
         *
         * @return This RequestBuilder
         */
        public RequestBuilder<ModelType> approximate() {
            return downsample(Downsampler.AT_LEAST);
        }

        /**
         * Load images at their original size using {@link Downsampler#NONE}.
         *
         * @see #downsample(com.bumptech.glide.resize.load.Downsampler)
         *
         * @return This RequestBuilder
         */
        public RequestBuilder<ModelType> asIs() {
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
         * @return This RequestBuilder
         */
        public RequestBuilder<ModelType> downsample(Downsampler downsampler) {
            super.imageDecoder(downsampler);
            return this;
        }

        public RequestBuilder<ModelType> thumbnail(float sizeMultiplier) {
            super.thumbnail(sizeMultiplier);
            return this;
        }

        public RequestBuilder<ModelType> thumbnail(RequestBuilder<ModelType> thumbnailRequest) {
            super.thumbnail(thumbnailRequest);
            return this;
        }

         public RequestBuilder<ModelType> sizeMultiplier(float sizeMultiplier) {
             super.sizeMultiplier(sizeMultiplier);
             return this;
         }

        @Override
        public RequestBuilder<ModelType> imageDecoder(BitmapDecoder<InputStream> decoder) {
            super.imageDecoder(decoder);
            return this;
        }

        @Override
        public RequestBuilder<ModelType> videoDecoder(BitmapDecoder<ParcelFileDescriptor> decoder) {
            super.videoDecoder(decoder);
            return this;
        }

        @Override
        public RequestBuilder<ModelType> centerCrop() {
            super.centerCrop();
            return this;
        }

        @Override
        public RequestBuilder<ModelType> fitCenter() {
            super.fitCenter();
            return this;
        }

        @Override
        public RequestBuilder<ModelType> transform(Transformation transformation) {
            super.transform(transformation);
            return this;
        }

        @Override
        public RequestBuilder<ModelType> animate(int animationId) {
            super.animate(animationId);
            return this;
        }

        @Override
        public RequestBuilder<ModelType> placeholder(int resourceId) {
            super.placeholder(resourceId);
            return this;
        }

        @Override
        public RequestBuilder<ModelType> error(int resourceId) {
            super.error(resourceId);
            return this;
        }

        @Override
        public RequestBuilder<ModelType> listener(RequestListener<ModelType> requestListener) {
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
    private static class GenericRequestBuilder<ModelType, ImageResourceType, VideoResourceType> {
        private Context context;
        private ModelLoaderFactory<ModelType, ImageResourceType> imageModelLoaderFactory;
        private final List<Transformation> transformations = new ArrayList<Transformation>();
        private final ModelLoaderFactory<ModelType, VideoResourceType> videoModelLoaderFactory;
        private final ModelType model;

        private int animationId;
        private int placeholderId;
        private int errorId;
        private RequestListener<ModelType> requestListener;
        private BitmapDecoder<ImageResourceType> imageDecoder;
        private BitmapDecoder<VideoResourceType> videoDecoder;
        private Float thumbSizeMultiplier;
        private GenericRequestBuilder<ModelType, ImageResourceType, VideoResourceType> thumbnailRequestBuilder;
        private Float sizeMultiplier = 1f;

        private GenericRequestBuilder(Context context, ModelType model,
                ModelLoaderFactory<ModelType, ImageResourceType> imageFactory,
                ModelLoaderFactory<ModelType, VideoResourceType> videoFactory) {
            if (context == null) {
                throw new NullPointerException("Context can't be null");
            }
            this.context = context;

            if (model == null ) {
                throw new NullPointerException("Model can't be null");
            }
            this.model = model;

            if (imageFactory == null && videoFactory == null) {
                throw new NullPointerException("No ModelLoaderFactorys registered for either image or video type,"
                        + " class=" + model.getClass());
            }
            this.imageModelLoaderFactory = imageFactory;
            this.videoModelLoaderFactory = videoFactory;
        }

        /**
         * Loads and displays the image retrieved by the given thumbnail request if it finishes before this request.
         * Best used for loading thumbnail images that are smaller and will be loaded more quickly than the fullsize
         * image. There are no guarantees about the order in which the requests will actually finish. However, if the
         * thumb request completes after the full request, the thumb image will never replace the full image.
         *
         * @see #thumbnail(float)
         *
         * <p>
         *     Note - Any options on the main request will not be passed on to the thumbnail request. For example, if
         *     you want an animation to occur when either the full image loads or the thumbnail loads, you need to call
         *     {@link #animate(int)} on both the thumb and the full request. For a simpler thumbnail option, see
         *     {@link #thumbnail(float)}.
         * </p>
         *
         * <p>
         *     Only the thumbnail call on the main request will be obeyed.
         * </p>
         *
         * @param thumbnailRequest The request to use to load the thumbnail.
         * @return This builder object.
         */
        public GenericRequestBuilder<ModelType, ImageResourceType, VideoResourceType> thumbnail(
                GenericRequestBuilder<ModelType, ImageResourceType, VideoResourceType> thumbnailRequest) {
            this.thumbnailRequestBuilder = thumbnailRequest;

            return this;
        }

        /**
         * Loads an image in an identical manner to this request except with the dimensions of the target multiplied
         * by the given size multiplier. If the thumbnail load completes before the fullsize load, the thumbnail will
         * be shown. If the thumbnail load completes afer the fullsize load, the thumbnail will not be shown.
         *
         * <p>
         *     Note - The thumbnail image will be smaller than the size requested so the target (or {@link ImageView})
         *     must be able to scale the thumbnail appropriately. See {@link ImageView.ScaleType}.
         * </p>
         *
         * <p>
         *     Almost all options will be copied from the original load, including the {@link ModelLoader},
         *     {@link BitmapDecoder}, and {@link Transformation}s. However, {@link #placeholder(int)} and
         *     {@link #error(int)}, and {@link #listener(RequestListener)} will only be used on the fullsize load and
         *     will not be copied for the thumbnail load.
         * </p>
         *
         * <p>
         *     Only the thumbnail call on the main request will be obeyed.
         * </p>
         *
         * @param sizeMultiplier The multiplier to apply to the {@link Target}'s dimensions when loading the thumbnail.
         * @return This builder object.
         */
        public GenericRequestBuilder<ModelType, ImageResourceType, VideoResourceType> thumbnail(float sizeMultiplier) {
            if (sizeMultiplier < 0f || sizeMultiplier > 1f) {
                throw new IllegalArgumentException("sizeMultiplier must be between 0 and 1");
            }
            this.thumbSizeMultiplier = sizeMultiplier;

            return this;
        }

        /**
         * Applies a multiplier to the {@link Target}'s size before loading the image. Useful for loading thumbnails
         * or trying to avoid loading huge bitmaps on devices with overly dense screens.
         *
         * @param sizeMultiplier The multiplier to apply to the {@link Target}'s dimensions when loading the image.
         * @return This builder object.
         */
        public GenericRequestBuilder<ModelType, ImageResourceType, VideoResourceType> sizeMultiplier(
                float sizeMultiplier) {
            if (sizeMultiplier < 0f || sizeMultiplier > 1f) {
                throw new IllegalArgumentException("sizeMultiplier must be between 0 and 1");
            }
            this.sizeMultiplier = sizeMultiplier;

            return this;
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
         * @return This RequestBuilder.
         */
        public GenericRequestBuilder<ModelType, ImageResourceType, VideoResourceType> imageDecoder(
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
        public GenericRequestBuilder<ModelType, ImageResourceType, VideoResourceType> videoDecoder(
                BitmapDecoder<VideoResourceType> decoder) {
            this.videoDecoder = decoder;

            return this;
        }

        /**
         * Transform images using {@link Transformation#CENTER_CROP}.
         *
         * @return This RequestBuilder
         */
        public GenericRequestBuilder<ModelType, ImageResourceType, VideoResourceType> centerCrop() {
            return transform(Transformation.CENTER_CROP);
        }

        /**
         * Transform images using {@link Transformation#FIT_CENTER}.
         *
         * @return This RequestBuilder
         */
        public GenericRequestBuilder<ModelType, ImageResourceType, VideoResourceType> fitCenter() {
            return transform(Transformation.FIT_CENTER);
        }


        /**
         * Transform images with the given {@link Transformation}. Appends this transformation onto any existing
         * transformations
         *
         * @param transformation the transformation to apply.
         * @return This RequestBuilder
         */
        public GenericRequestBuilder<ModelType, ImageResourceType, VideoResourceType> transform(
                Transformation transformation) {
            transformations.add(transformation);

            return this;
        }

        /**
         * Sets an animation to run on the wrapped target when an image load finishes. Will only be run if the image
         * was loaded asynchronously (ie was not in the memory cache)
         *
         * @param animationId The resource id of the animation to run
         * @return This RequestBuilder
         */
        public GenericRequestBuilder<ModelType, ImageResourceType, VideoResourceType> animate(int animationId) {
            this.animationId = animationId;

            return this;
        }

        /**
         * Sets a resource to display while an image is loading
         *
         * @param resourceId The id of the resource to use as a placeholder
         * @return This RequestBuilder
         */
        public GenericRequestBuilder<ModelType, ImageResourceType, VideoResourceType> placeholder(int resourceId) {
            this.placeholderId = resourceId;

            return this;
        }

        /**
         * Sets a resource to display if a load fails
         *
         * @param resourceId The id of the resource to use as a placeholder
         * @return This request
         */
        public GenericRequestBuilder<ModelType, ImageResourceType, VideoResourceType> error(int resourceId) {
            this.errorId = resourceId;

            return this;
        }

        /**
         * Sets a RequestBuilder listener to monitor the image load. It's best to create a single instance of an
         * exception handler per type of request (usually activity/fragment) rather than pass one in per request to
         * avoid some redundant object allocation.
         *
         * @param requestListener The request listener to use
         * @return This request
         */
        public GenericRequestBuilder<ModelType, ImageResourceType, VideoResourceType> listener(
                RequestListener<ModelType> requestListener) {
            this.requestListener = requestListener;

            return this;
        }

        /**
         * Set the target the image will be loaded into.
         *
         * @param target The target to load te image for
         * @return The given target.
         */
        public <Y extends Target> Y into(Y target) {
            Request previous = target.getRequest();
            if (previous != null) {
                previous.clear();
            }

            Request request = buildRequest(target);
            target.setRequest(request);
            request.run();
            return target;
        }

        /**
         * Sets the {@link ImageView} the image will be loaded into, cancels any existing loads into the view, and frees
         * any resources Glide has loaded into the view so they may be reused.
         *
         * @see #clear(View)
         *
         * @param view The view to cancel previous loads for and load the new image into.
         * @return The {@link ImageViewTarget} used to wrap the given {@link ImageView}.
         */
        public ImageViewTarget into(ImageView view) {
            return into(new ImageViewTarget(view));
        }

        private <Y extends Target> Request buildRequest(Y target) {
            final Request result;
            if (thumbnailRequestBuilder != null) {
                ThumbnailRequestCoordinator requestCoordinator = new ThumbnailRequestCoordinator();
                Request fullRequest = buildBitmapRequest(target)
                        .setRequestCoordinator(requestCoordinator)
                        .build();

                if (thumbnailRequestBuilder.animationId == 0 && animationId != 0) {
                    thumbnailRequestBuilder.animationId = animationId;
                }

                if (thumbnailRequestBuilder.requestListener == null && requestListener != null) {
                    thumbnailRequestBuilder.requestListener = requestListener;
                }
                Request thumbnailRequest = thumbnailRequestBuilder.buildBitmapRequest(target)
                        .setRequestCoordinator(requestCoordinator)
                        .build();

                requestCoordinator.setRequests(fullRequest, thumbnailRequest);
                result = requestCoordinator;
            } else if (thumbSizeMultiplier != null) {
                ThumbnailRequestCoordinator requestCoordinator = new ThumbnailRequestCoordinator();
                Request fullRequest = buildBitmapRequest(target)
                        .setRequestCoordinator(requestCoordinator)
                        .build();
                Request thumbnailRequest = buildBitmapRequest(target)
                        .setRequestCoordinator(requestCoordinator)
                        .setSizeMultiplier(thumbSizeMultiplier)
                        .build();
                requestCoordinator.setRequests(fullRequest, thumbnailRequest);
                result = requestCoordinator;
            } else {
                result = buildBitmapRequest(target).build();
            }
            return result;
        }

        private <Y extends Target> BitmapRequestBuilder<ModelType> buildBitmapRequest(Y target) {
             ModelLoader<ModelType, ImageResourceType> imageModelLoader = null;
            if (imageModelLoaderFactory != null) {
                imageModelLoader = imageModelLoaderFactory.build(context, Glide.get(context).loaderFactory);
            }
            ModelLoader<ModelType, VideoResourceType> videoModelLoader = null;
            if (videoModelLoaderFactory != null) {
                videoModelLoader = videoModelLoaderFactory.build(context, Glide.get(context).loaderFactory);
            }
            final Transformation transformation = getFinalTransformation();

            return new BitmapRequestBuilder<ModelType>()
                    .setContext(context)
                    .setPriority(Priority.NORMAL)
                    .setImageManager(Glide.get(context).imageManager)
                    .setModel(model)
                    .setTarget(target)
                    .setBitmapLoadFactory(
                            new ImageVideoBitmapLoadFactory<ModelType, ImageResourceType, VideoResourceType>(
                                    imageModelLoader != null && imageDecoder != null ?
                                    new ResourceBitmapLoadFactory<ModelType, ImageResourceType>(
                                            imageModelLoader, imageDecoder) : null,
                                    videoModelLoader != null && videoDecoder != null ?
                                    new ResourceBitmapLoadFactory<ModelType, VideoResourceType>(
                                            videoModelLoader, videoDecoder) : null,
                                    transformation))
                    .setAnimation(animationId)
                    .setRequestListener(requestListener)
                    .setPlaceholderResource(placeholderId)
                    .setErrorResource(errorId)
                    .setSizeMultiplier(sizeMultiplier);
        }

        private Transformation getFinalTransformation() {
            switch (transformations.size()) {
                case 0:
                    return Transformation.NONE;
                case 1:
                    return transformations.get(0);
                default:
                    return new MultiTransformation(transformations);
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

    private static class CancelTarget extends ViewTarget<View> {
        public CancelTarget(View view) {
            super(view);
        }

        @Override
        public void onImageReady(Bitmap bitmap) { }

        @Override
        public void setPlaceholder(Drawable placeholder) { }
    }
}
