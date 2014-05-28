package com.bumptech.glide;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import com.android.volley.RequestQueue;
import com.bumptech.glide.loader.GlideUrl;
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
import com.bumptech.glide.loader.bitmap.model.stream.StreamUrlLoader;
import com.bumptech.glide.resize.Engine;
import com.bumptech.glide.resize.RequestContext;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.resize.cache.DiskCache;
import com.bumptech.glide.resize.cache.MemoryCache;
import com.bumptech.glide.resize.request.Request;
import com.bumptech.glide.resize.target.Target;
import com.bumptech.glide.resize.target.ViewTarget;
import com.bumptech.glide.volley.VolleyUrlLoader;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

/**
 * A singleton to present a simple static interface for building requests with {@link RequestBuilder} and maintaining
 * an {@link Engine}, {@link BitmapPool}, {@link DiskCache} and {@link MemoryCache}.
 *
 * <p>
 * Note - This class is not thread safe.
 * </p>
 */
public class Glide {
    public static boolean CAN_REUSE_BITMAPS = Build.VERSION.SDK_INT >= 11;

    // 250 MB
    static final int DEFAULT_DISK_CACHE_SIZE = 250 * 1024 * 1024;

    private static final String DEFAULT_DISK_CACHE_DIR = "image_manager_disk_cache";
    private static final String TAG = "Glide";
    private static final float MEMORY_SIZE_RATIO = 1f/10f;
    private static Glide GLIDE;

    private final GenericLoaderFactory loaderFactory = new GenericLoaderFactory();
    private final RequestQueue requestQueue;
    private final Engine engine;
    private final RequestContext requestContext;
    private final BitmapPool bitmapPool;
    private final MemoryCache memoryCache;

    /**
     * Returns true if this device is a low ram device or has an older sdk version and likely has relatively little
     * available ram.
     *
     * @see ActivityManager#isLowRamDevice()
     */
    @TargetApi(19)
    public static boolean isLowMemoryDevice(Context context) {
        final ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return Build.VERSION.SDK_INT < 11 ||
                (Build.VERSION.SDK_INT >= 19 && activityManager.isLowRamDevice());
    }

    /**
     * Get the maximum safe memory cache size for this particular device based on the # of mb allocated to each app.
     * This is a conservative estimate that has been safe for 2.2+ devices consistently. It is probably rather small
     * for newer devices.
     *
     * @param context A context
     * @return The maximum safe size for the memory cache for this devices in bytes
     */
    public static int getSafeMemoryCacheSize(Context context){
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return Math.round(MEMORY_SIZE_RATIO * activityManager.getMemoryClass() * 1024 * 1024);
    }

    /**
     * Try to get the external cache directory if available and default to the internal. Use a default name for the
     * cache directory if no name is provided
     *
     * @param context A context
     * @return A File representing the default disk cache directory
     */
    public static File getPhotoCacheDir(Context context) {
        return getPhotoCacheDir(context, DEFAULT_DISK_CACHE_DIR);
    }

    /**
     * Try to get the external cache directory if available and default to the internal. Use a default name for the
     * cache directory if no name is provided
     *
     * @param context A context
     * @param cacheName The name of the subdirectory in which to store the cache
     * @return A File representing the default disk cache directory
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File getPhotoCacheDir(Context context, String cacheName) {
        File cacheDir = context.getCacheDir();
        if (cacheDir != null) {
            File result = new File(cacheDir, cacheName);
            result.mkdirs();
            return result;
        }
        if (Log.isLoggable(TAG, Log.ERROR)) {
            Log.e(TAG, "default disk cache dir is null");
        }
        return null;
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
        if (isSetup()) {
            throw new IllegalArgumentException("Glide is already setup, check with isSetup() first");
        }

        GLIDE = builder.createGlide();
    }

    static void tearDown() {
        GLIDE = null;
    }

    Glide(Engine engine, RequestQueue requestQueue, RequestContext requestContext,
            MemoryCache memoryCache, BitmapPool bitmapPool) {
        this.engine = engine;
        this.requestQueue = requestQueue;
        this.requestContext = requestContext;
        this.bitmapPool = bitmapPool;
        this.memoryCache = memoryCache;
        register(File.class, ParcelFileDescriptor.class, new FileDescriptorFileLoader.Factory());
        register(File.class, InputStream.class, new StreamFileLoader.Factory());
        register(Integer.class, ParcelFileDescriptor.class, new FileDescriptorResourceLoader.Factory());
        register(Integer.class, InputStream.class, new StreamResourceLoader.Factory());
        register(String.class, ParcelFileDescriptor.class, new FileDescriptorStringLoader.Factory());
        register(String.class, InputStream.class, new StreamStringLoader.Factory());
        register(Uri.class, ParcelFileDescriptor.class, new FileDescriptorUriLoader.Factory());
        register(Uri.class, InputStream.class, new StreamUriLoader.Factory());
        register(URL.class, InputStream.class, new StreamUrlLoader.Factory());
        register(GlideUrl.class, InputStream.class, new VolleyUrlLoader.Factory(requestQueue));
    }

    Engine getEngine() {
        return engine;
    }

    BitmapPool getBitmapPool() {
        return bitmapPool;
    }

    private GenericLoaderFactory getLoaderFactory() {
        return loaderFactory;
    }

    /**
     * Returns the {@link RequestQueue} Glide is using to fetch images over http/https.
     */
    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    /**
     * Clears as much memory as possible.
     *
     * @see ComponentCallbacks2#onLowMemory()
     */
    public void clearMemory() {
        bitmapPool.clearMemory();
        memoryCache.clearMemory();
    }

    /**
     * Clears some memory with the exact amount depending on the given level.
     *
     * @see ComponentCallbacks2#onTrimMemory(int)
     */
    public void trimMemory(int level) {
        bitmapPool.trimMemory(level);
        memoryCache.trimMemory(level);
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
        Target viewTarget = new ClearTarget(view);
        clear(viewTarget);
    }

    /**
     * Use the given factory to build a {@link ModelLoader} for models of the given class. Generally the best use of
     * this method is to replace one of the default factories or add an implementation for other similar low level
     * models. Typically the {@link ModelRequest#using(StreamModelLoader)} or
     * {@link ModelRequest#using(FileDescriptorModelLoader)} syntax is preferred because it directly links the model
     * with the ModelLoader being used to load it. Any factory replaced by the given factory will have its
     * {@link ModelLoaderFactory#teardown()}} method called.
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

    /**
     * Removes any {@link ModelLoaderFactory} registered for the given model and resource classes if one exists. If a
     * {@link ModelLoaderFactory} is removed, its {@link ModelLoaderFactory#teardown()}} method will be called.
     *
     * @param modelClass The model class.
     * @param resourceClass The resource class.
     * @param <T> The type of the model.
     * @param <Y> The type of the resource.
     */
    public <T, Y> void unregister(Class<T> modelClass, Class<Y> resourceClass) {
        ModelLoaderFactory<T, Y> removed = loaderFactory.unregister(modelClass, resourceClass);
        if (removed != null) {
            removed.teardown();
        }
    }

    /**
     * Build a {@link ModelLoader} for the given model class using registered {@link ModelLoaderFactory}s.
     *
     * @see  #buildModelLoader(Object, Class, Context)
     * @see  #buildStreamModelLoader(Class, Context)
     * @see  #buildFileDescriptorModelLoader(Class, Context)
     *
     * @param modelClass The class to get a {@link ModelLoader} for.
     * @param resourceClass The resource class to get a {@link ModelLoader} for.
     * @param context Any context.
     * @param <T> The type of the model.
     * @param <Y> The type of the resource.
     * @return A new {@link ModelLoader} for the given model class.
     */
    public static <T, Y> ModelLoader<T, Y> buildModelLoader(Class<T> modelClass, Class<Y> resourceClass,
            Context context) {
        return Glide.get(context).getLoaderFactory().buildModelLoader(modelClass, resourceClass, context);
    }

    /**
     * A convenience method to build a {@link ModelLoader} for a given model object using registered
     * {@link ModelLoaderFactory}s.
     *
     * @see #buildModelLoader(Class, Class, Context)
     *
     * @param model A non null model object whose class we will get a {@link ModelLoader} for.
     * @param resourceClass The resource class to get a {@link ModelLoader} for.
     * @param context Any context.
     * @param <T> The type of the model.
     * @param <Y> The type of the resource.
     * @return A new {@link ModelLoader} for the given model and resource classes, or null if model is null.
     */
    @SuppressWarnings("unchecked")
    public static <T, Y> ModelLoader<T, Y> buildModelLoader(T model, Class<Y> resourceClass, Context context) {
        if (model == null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Unable to load null model, setting placeholder only");
            }
            return null;
        }
        return buildModelLoader((Class<T>) model.getClass(), resourceClass, context);
    }

    /**
     * A method to build a {@link ModelLoader} for the given model that produces {@link InputStream}s using a registered
     * factory.
     *
     * @see #buildModelLoader(Class, Class, android.content.Context)
     */
    public static <T> ModelLoader<T, InputStream> buildStreamModelLoader(Class<T> modelClass, Context context) {
        return buildModelLoader(modelClass, InputStream.class, context);
    }

    /**
     * A method to build a {@link ModelLoader} for the given model that produces {@link InputStream}s using a registered
     * factory.
     *
     * @see #buildModelLoader(Object, Class, Context)
     */
    public static <T> ModelLoader<T, InputStream> buildStreamModelLoader(T model, Context context) {
        return buildModelLoader(model, InputStream.class, context);
    }

    /**
     * A method to build a {@link ModelLoader} for the given model class that produces
     * {@link ParcelFileDescriptor}s using a registered factory.
     *
     * @see #buildModelLoader(Class, Class, android.content.Context)
     */
    public static <T> ModelLoader<T, ParcelFileDescriptor> buildFileDescriptorModelLoader(Class<T> modelClass,
            Context context) {
        return buildModelLoader(modelClass, ParcelFileDescriptor.class, context);
    }

    /**
     * A method to build a {@link ModelLoader} for the given model class that produces
     * {@link ParcelFileDescriptor}s using a registered factory.
     *
     * @see #buildModelLoader(Object, Class, android.content.Context)
     */
    public static <T> ModelLoader<T, ParcelFileDescriptor> buildFileDescriptorModelLoader(T model, Context context) {
        return buildModelLoader(model, ParcelFileDescriptor.class, context);
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
        private final RequestContext requestContext;

        private ModelRequest(Context context) {
            this.context = context;
            this.requestContext = Glide.get(context).buildContext();
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
            return new ImageModelRequest<T>(context, requestContext, modelLoader);
        }

        /**
         * A convenience method to use a {@link StreamByteArrayLoader} to decode an image from a byte array.
         *
         * @param modelLoader The byte array loader.
         * @return A new {@link ImageModelRequest}.
         */
        public ImageModelRequest<byte[]> using(StreamByteArrayLoader modelLoader) {
            return new ImageModelRequest<byte[]>(context, requestContext, modelLoader);
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
            return new VideoModelRequest<T>(context, requestContext, modelLoader);
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
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the model
         * into.
         */
        public RequestBuilder<String> load(String string) {
            return loadGeneric(string);
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for {@link Uri} to load the image at the given uri.
         * Defaults to {@link StreamUriLoader.Factory} and {@link StreamUriLoader}.
         *
         * @see #using(StreamModelLoader)
         *
         * @param uri The uri representing the image. Must be a uri handled by {@link StreamUriLoader}
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the model
         * into.
         */
        public RequestBuilder<Uri> load(Uri uri) {
            return loadGeneric(uri);
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for {@link File} to load the image represented by the
         * given {@link File}. Defaults to {@link StreamFileLoader.Factory} and {@link StreamFileLoader} to load the
         * given model.
         *
         * @see #using(StreamModelLoader)
         *
         * @param file The File containing the image
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the model
         * into.
         */
        public RequestBuilder<File> load(File file) {
            return loadGeneric(file);
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for {@link Integer} to load the image represented by
         * the given {@link Integer} resource id. Defaults to {@link StreamResourceLoader.Factory} and
         * {@link StreamResourceLoader} to load the given model.
         *
         * @see #using(StreamModelLoader)
         *
         * @param resourceId the id of the resource containing the image
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the model
         * into.
         */
        public RequestBuilder<Integer> load(Integer resourceId) {
            return loadGeneric(resourceId);
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for the given model type to load the image
         * represented by the given model.
         *
         * @param model The model to load.
         * @param <T> The type of the model to load.
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the image
         * into.
         */
        @SuppressWarnings("unused")
        public <T> RequestBuilder<T> loadFromImage(T model) {
            return loadGeneric(model);
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for {@link URL} to load the image represented by the
         * given {@link URL}. Defaults to {@link VolleyUrlLoader.Factory} and {@link VolleyUrlLoader} to load the given
         * model.
         *
         * @see #using(StreamModelLoader)
         *
         * @param url The URL representing the image.
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the model
         * into.
         */
        public RequestBuilder<URL> loadFromImage(URL url) {
            return loadGeneric(url);
        }

        /**
         * Use a new {@link StreamByteArrayLoader} to load an image from the given model.
         *
         * @see #loadFromImage(byte[])
         *
         * @param model The data to load.
         * @param id A unique id that identifies the image represented by the model suitable for use as a cache key
         *           (url, filepath etc). If there is no suitable id, use {@link #loadFromImage(byte[])} instaed.
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the image
         * into.
         */
        public RequestBuilder<byte[]> loadFromImage(byte[] model, final String id) {
            requestContext.register(new StreamByteArrayLoader() {
                @Override
                public String getId(byte[] model) {
                    return id;
                }
            }, byte[].class, InputStream.class);

            return new RequestBuilder<byte[]>(context, model, requestContext);
        }

        /**
         * Use a new {@link StreamByteArrayLoader} to load an image from the given model. Suitable when there is no
         * simple id that represents the given data.
         *
         * @param model the data to load.
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the image
         * into.
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
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the image
         * into.
         */
        @SuppressWarnings("unused")
        public <T> RequestBuilder<T> loadFromVideo(T model) {
            return loadGeneric(model);
        }

        /**
         * Use the {@link ModelLoaderFactory}s currently registered for the given model type for
         * {@link InputStream}s and {@link ParcelFileDescriptor}s to load a thumbnail from either the image or the video
         * represented by the given model.
         *
         * @param model The model the load.
         * @param <T> The type of the model to load.
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the image
         * into.
         */
        public <T> RequestBuilder<T> load(T model) {
            return loadGeneric(model);
        }

        private <T> RequestBuilder<T> loadGeneric(T model) {
            if (model != null) {
                ModelLoader<T, InputStream> streamModelLoader = buildStreamModelLoader(model, context);
                if (streamModelLoader != null) {
                    requestContext.register(streamModelLoader, (Class<T>) model.getClass(), InputStream.class);
                }

                ModelLoader<T, ParcelFileDescriptor> fileDescriptorModelLoader =
                        buildFileDescriptorModelLoader(model, context);
                if (fileDescriptorModelLoader != null) {
                    requestContext.register(fileDescriptorModelLoader, (Class<T>) model.getClass(),
                            ParcelFileDescriptor.class);
                }
            }
            return new RequestBuilder<T>(context, model, requestContext);
        }
    }

    private RequestContext buildContext() {
        return new RequestContext(requestContext);
    }

    /**
     * A helper class for building requests with custom {@link ModelLoader}s that translate models to
     * {@link ParcelFileDescriptor} resources for loading video thumbnails.
     *
     * @param <T> The type of the model.
     */
    public static class VideoModelRequest<T> {
        private final Context context;
        private final ModelLoader<T, ParcelFileDescriptor> loader;
        private final RequestContext requestContext;

        private VideoModelRequest(Context context, RequestContext requestContext,
                ModelLoader<T, ParcelFileDescriptor> loader) {
            this.context = context;
            this.requestContext = requestContext;
            this.loader = loader;
        }

        public RequestBuilder<T> loadFromVideo(T model) {
            requestContext.register(loader, (Class<T>) model.getClass(), ParcelFileDescriptor.class);
            return new RequestBuilder<T>(context, model, requestContext);
        }
    }

    /**
     * A helper class for building requests with custom {@link ModelLoader}s that translate models to
     * {@link InputStream} resources for loading images.
     *
     * @param <T> The type of the model.
     */
    public static class ImageModelRequest<T> {
        private final Context context;
        private RequestContext requestContext;
        private final ModelLoader<T, InputStream> loader;

        private ImageModelRequest(Context context, RequestContext requestContext, ModelLoader<T, InputStream> loader) {
            this.context = context;
            this.requestContext = requestContext;
            this.loader = loader;
        }

        public RequestBuilder<T> load(T model) {
            requestContext.register(loader, (Class <T>) model.getClass(), InputStream.class);
            return new RequestBuilder<T>(context, model, requestContext);
        }
    }

    private static class ClearTarget extends ViewTarget<View> {
        public ClearTarget(View view) {
            super(view);
        }

        @Override
        public void onImageReady(Bitmap bitmap) { }

        @Override
        public void setPlaceholder(Drawable placeholder) { }
    }
}
