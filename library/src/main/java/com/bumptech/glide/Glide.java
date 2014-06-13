package com.bumptech.glide;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.android.volley.RequestQueue;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.file_descriptor.FileDescriptorFileLoader;
import com.bumptech.glide.load.model.file_descriptor.FileDescriptorModelLoader;
import com.bumptech.glide.load.model.file_descriptor.FileDescriptorResourceLoader;
import com.bumptech.glide.load.model.file_descriptor.FileDescriptorStringLoader;
import com.bumptech.glide.load.model.file_descriptor.FileDescriptorUriLoader;
import com.bumptech.glide.load.model.stream.StreamFileLoader;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.bumptech.glide.load.model.stream.StreamResourceLoader;
import com.bumptech.glide.load.model.stream.StreamStringLoader;
import com.bumptech.glide.load.model.stream.StreamUriLoader;
import com.bumptech.glide.load.model.stream.StreamUrlLoader;
import com.bumptech.glide.load.resource.bitmap.FileDescriptorBitmapDataLoadProvider;
import com.bumptech.glide.load.resource.bitmap.ImageVideoDataLoadProvider;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDataLoadProvider;
import com.bumptech.glide.load.resource.gif.GifDataLoadProvider;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapper;
import com.bumptech.glide.load.resource.gifbitmap.ImageVideoGifDataLoadProvider;
import com.bumptech.glide.load.resource.transcode.BitmapDrawableTranscoder;
import com.bumptech.glide.load.resource.transcode.GifBitmapDrawableTranscoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.load.resource.transcode.TranscoderFactory;
import com.bumptech.glide.provider.DataLoadProviderFactory;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.ImageViewTargetFactory;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.volley.VolleyUrlLoader;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * A singleton to present a simple static interface for building requests with {@link BitmapRequestBuilder} and maintaining
 * an {@link Engine}, {@link BitmapPool}, {@link DiskCache} and {@link MemoryCache}.
 *
 * <p>
 * Note - This class is not thread safe.
 * </p>
 */
public class Glide {
    // 250 MB
    static final int DEFAULT_DISK_CACHE_SIZE = 250 * 1024 * 1024;

    private static final String DEFAULT_DISK_CACHE_DIR = "image_manager_disk_cache";
    private static final String TAG = "Glide";
    private static Glide GLIDE;

    private final GenericLoaderFactory loaderFactory = new GenericLoaderFactory();
    private final RequestQueue requestQueue;
    private final Engine engine;
    private final BitmapPool bitmapPool;
    private final MemoryCache memoryCache;
    private final ImageViewTargetFactory imageViewTargetFactory = new ImageViewTargetFactory();
    private final TranscoderFactory transcoderFactory = new TranscoderFactory();
    private final DataLoadProviderFactory dataLoadProviderFactory;

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

    Glide(Engine engine, RequestQueue requestQueue, MemoryCache memoryCache, BitmapPool bitmapPool,
            Context context) {
        this.engine = engine;
        this.requestQueue = requestQueue;
        this.bitmapPool = bitmapPool;
        this.memoryCache = memoryCache;

        dataLoadProviderFactory = new DataLoadProviderFactory();
        dataLoadProviderFactory.register(InputStream.class, Bitmap.class, new StreamBitmapDataLoadProvider(bitmapPool));

        dataLoadProviderFactory.register(ParcelFileDescriptor.class, Bitmap.class,
                new FileDescriptorBitmapDataLoadProvider(bitmapPool));

        ImageVideoDataLoadProvider imageVideoDataLoadProvider = new ImageVideoDataLoadProvider(bitmapPool);
        dataLoadProviderFactory.register(ImageVideoWrapper.class, Bitmap.class, imageVideoDataLoadProvider);

        GifDataLoadProvider gifDataLoadProvider = new GifDataLoadProvider(context, bitmapPool);
        dataLoadProviderFactory.register(ImageVideoWrapper.class, GifBitmapWrapper.class,
                new ImageVideoGifDataLoadProvider(imageVideoDataLoadProvider, gifDataLoadProvider));

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

        transcoderFactory.register(Bitmap.class, BitmapDrawable.class,
                new BitmapDrawableTranscoder(context.getResources(), bitmapPool));
        transcoderFactory.register(GifBitmapWrapper.class, Drawable.class,
                new GifBitmapDrawableTranscoder(context));
    }

    public BitmapPool getBitmapPool() {
        return bitmapPool;
    }

    <Z, R> ResourceTranscoder<Z, R> buildTranscoder(Class<Z> decodedClass, Class<R> transcodedClass) {
        return transcoderFactory.get(decodedClass, transcodedClass);
    }

    <T, Z> DataLoadProvider<T, Z> buildDataProvider(Class<T> dataClass, Class<Z> decodedClass) {
        return dataLoadProviderFactory.get(dataClass, decodedClass);
    }

    <R> Target<R> buildImageViewTarget(ImageView imageView, Class<R> transcodedClass) {
        return imageViewTargetFactory.buildTarget(imageView, transcodedClass);
    }

    Engine getEngine() {
        return engine;
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
     * Adjusts Glide's current and maximum memory usage based on the given {@link MemoryCategory}.
     *
     * <p>
     *     The default {@link MemoryCategory} is {@link MemoryCategory#NORMAL}. {@link MemoryCategory#HIGH} increases
     *     Glide's maximum memory usage by up to 50% and {@link MemoryCategory#LOW} decreases Glide's maximum memory
     *     usage by 50%. This method should be used to temporarily increase or decrease memory useage for a single
     *     Activity or part of the app. Use {@link GlideBuilder#setMemoryCache(MemoryCache)} to set a permanent
     *     memory size if you want to change the default.
     * </p>
     */
    public void setMemoryCategory(MemoryCategory memoryCategory) {
        memoryCache.setSizeMultiplier(memoryCategory.getMultiplier());
        bitmapPool.setSizeMultiplier(memoryCategory.getMultiplier());
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
        return new ModelRequest(context, Glide.get(context));
    }

    private static class ClearTarget extends ViewTarget<View, Object> {
        public ClearTarget(View view) {
            super(view);
        }

        @Override
        public void onResourceReady(Object resource) { }

        @Override
        public void setPlaceholder(Drawable placeholder) { }
    }
}
