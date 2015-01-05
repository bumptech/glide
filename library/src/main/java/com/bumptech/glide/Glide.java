package com.bumptech.glide;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.data.DataRewinderRegistry;
import com.bumptech.glide.load.data.InputStreamRewinder;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.engine.prefill.BitmapPreFiller;
import com.bumptech.glide.load.engine.prefill.PreFillType;
import com.bumptech.glide.load.model.AssetUriLoader;
import com.bumptech.glide.load.model.FileLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.ModelLoaderRegistry;
import com.bumptech.glide.load.model.ResourceLoader;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.model.StringLoader;
import com.bumptech.glide.load.model.UriLoader;
import com.bumptech.glide.load.model.stream.HttpGlideUrlLoader;
import com.bumptech.glide.load.model.stream.HttpUriLoader;
import com.bumptech.glide.load.model.stream.MediaStoreImageThumbLoader;
import com.bumptech.glide.load.model.stream.MediaStoreVideoThumbLoader;
import com.bumptech.glide.load.model.stream.ByteArrayLoader;
import com.bumptech.glide.load.model.stream.UrlLoader;
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableDecoder;
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableEncoder;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import com.bumptech.glide.load.resource.bitmap.FileDescriptorBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.load.resource.file.FileDecoder;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gif.GifFrameModelLoader;
import com.bumptech.glide.load.resource.gif.GifFrameResourceDecoder;
import com.bumptech.glide.load.resource.gif.GifResourceDecoder;
import com.bumptech.glide.load.resource.gif.GifResourceEncoder;
import com.bumptech.glide.load.resource.transcode.BitmapBytesTranscoder;
import com.bumptech.glide.load.resource.transcode.BitmapDrawableTranscoder;
import com.bumptech.glide.load.resource.transcode.GifDrawableBytesTranscoder;
import com.bumptech.glide.load.resource.transcode.TranscoderRegistry;
import com.bumptech.glide.manager.RequestManagerRetriever;
import com.bumptech.glide.module.GlideModule;
import com.bumptech.glide.module.ManifestParser;
import com.bumptech.glide.provider.EncoderRegistry;
import com.bumptech.glide.provider.ResourceDecoderRegistry;
import com.bumptech.glide.provider.ResourceEncoderRegistry;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.GlideContext;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.ImageViewTargetFactory;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.util.Util;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * A singleton to present a simple static interface for building requests with {@link RequestBuilder} and
 * maintaining an {@link Engine}, {@link BitmapPool}, {@link com.bumptech.glide.load.engine.cache.DiskCache} and
 * {@link MemoryCache}.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class Glide implements ComponentCallbacks2 {
    /** 250 MB of cache. */
    static final int DEFAULT_DISK_CACHE_SIZE = 250 * 1024 * 1024;

    private static final String DEFAULT_DISK_CACHE_DIR = "image_manager_disk_cache";
    private static final String TAG = "Glide";
    private static volatile Glide glide;

    private final BitmapPool bitmapPool;
    private final MemoryCache memoryCache;
    private final BitmapPreFiller bitmapPreFiller;
    private final ModelLoaderRegistry modelLoaderRegistry;
    private final GlideContext glideContext;

    /**
     * Returns a directory with a default name in the private cache directory of the application to use to store
     * retrieved media and thumbnails.
     *
     * @see #getPhotoCacheDir(android.content.Context, String)
     *
     * @param context A context.
     */
    public static File getPhotoCacheDir(Context context) {
        return getPhotoCacheDir(context, DEFAULT_DISK_CACHE_DIR);
    }

    /**
     * Returns a directory with the given name in the private cache directory of the application to use to store
     * retrieved media and thumbnails.
     *
     * @see #getPhotoCacheDir(android.content.Context)
     *
     * @param context A context.
     * @param cacheName The name of the subdirectory in which to store the cache.
     */
    public static File getPhotoCacheDir(Context context, String cacheName) {
        File cacheDir = context.getCacheDir();
        if (cacheDir != null) {
            File result = new File(cacheDir, cacheName);
            if (!result.mkdirs() && (!result.exists() || !result.isDirectory())) {
                // File wasn't able to create a directory, or the result exists but not a directory
                return null;
            }
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
        if (glide == null) {
            synchronized (Glide.class) {
                if (glide == null) {
                    Context applicationContext = context.getApplicationContext();
                    List<GlideModule> modules = new ManifestParser(applicationContext).parse();

                    GlideBuilder builder = new GlideBuilder(applicationContext);
                    for (GlideModule module : modules) {
                        module.applyOptions(applicationContext, builder);
                    }
                    glide = builder.createGlide();
                    for (GlideModule module : modules) {
                        module.registerComponents(applicationContext, glide);
                    }
                }
            }
        }

        return glide;
    }

    /**
     * Returns false if the {@link Glide} singleton has not yet been created and can therefore be setup using
     * {@link #setup(GlideBuilder)}.
     *
     * @see #setup(GlideBuilder)
     *
     * @deprecated Use {@link com.bumptech.glide.module.GlideModule} instead. Scheduled to be removed in Glide 4.0.
     */
    @Deprecated
    public static boolean isSetup() {
        return glide != null;
    }

    /**
     * Creates the {@link Glide} singleton using the given builder. Can be used to set options like cache sizes and
     * locations.
     *
     * @see #isSetup()
     *
     * @deprecated Use {@link com.bumptech.glide.module.GlideModule} instead. Scheduled to be removed in Glide 4.0.
     * @param builder The builder.
     * @throws IllegalArgumentException if the Glide singleton has already been created.
     */
    @Deprecated
    public static void setup(GlideBuilder builder) {
        if (isSetup()) {
            throw new IllegalArgumentException("Glide is already setup, check with isSetup() first");
        }

        glide = builder.createGlide();
    }

    // For testing.
    static void tearDown() {
        glide = null;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    Glide(Engine engine, MemoryCache memoryCache, BitmapPool bitmapPool, Context context, DecodeFormat decodeFormat) {
        this.bitmapPool = bitmapPool;
        this.memoryCache = memoryCache;
        modelLoaderRegistry = new ModelLoaderRegistry(context);
        bitmapPreFiller = new BitmapPreFiller(memoryCache, bitmapPool, decodeFormat);

        EncoderRegistry encoderRegistry = new EncoderRegistry();
        encoderRegistry.add(InputStream.class, new StreamEncoder());

        ResourceEncoderRegistry resourceEncoderRegistry = new ResourceEncoderRegistry();
        ResourceDecoderRegistry decoderRegistry = new ResourceDecoderRegistry();

        /* Bitmaps */
        decoderRegistry.append(new StreamBitmapDecoder(bitmapPool, decodeFormat), InputStream.class, Bitmap.class);
        decoderRegistry.append(new FileDescriptorBitmapDecoder(bitmapPool, decodeFormat), ParcelFileDescriptor.class,
                Bitmap.class);
        resourceEncoderRegistry.add(Bitmap.class, new BitmapEncoder());

        /* GlideBitmapDrawables */
        decoderRegistry.append(new BitmapDrawableDecoder<InputStream>(context.getResources(), bitmapPool, new
                StreamBitmapDecoder(bitmapPool, decodeFormat)), InputStream.class, BitmapDrawable.class);
        decoderRegistry.append(new BitmapDrawableDecoder<ParcelFileDescriptor>(context.getResources(), bitmapPool,
                new FileDescriptorBitmapDecoder(bitmapPool, decodeFormat)), ParcelFileDescriptor.class,
                BitmapDrawable.class);
        resourceEncoderRegistry.add(BitmapDrawable.class, new BitmapDrawableEncoder(bitmapPool, new BitmapEncoder()));

        /* Gifs */
        decoderRegistry.prepend(new GifResourceDecoder(context, bitmapPool), InputStream.class, GifDrawable.class);
        resourceEncoderRegistry.add(GifDrawable.class, new GifResourceEncoder(bitmapPool));

        /* Gif Frames */
        modelLoaderRegistry.append(GifDecoder.class, GifDecoder.class, new GifFrameModelLoader.Factory());
        decoderRegistry.append(new GifFrameResourceDecoder(bitmapPool), GifDecoder.class, Bitmap.class);

        /* Files */
        decoderRegistry.append(new FileDecoder(), File.class, File.class);

        DataRewinderRegistry dataRewinderRegistry = new DataRewinderRegistry();
        dataRewinderRegistry.register(new InputStreamRewinder.Factory());

        modelLoaderRegistry.append(File.class, InputStream.class, new FileLoader.StreamFactory());
        modelLoaderRegistry.append(File.class, ParcelFileDescriptor.class, new FileLoader.FileDescriptorFactory());
        modelLoaderRegistry.append(int.class, InputStream.class, new ResourceLoader.StreamFactory());
        modelLoaderRegistry.append(int.class, ParcelFileDescriptor.class, new ResourceLoader.FileDescriptorFactory());
        modelLoaderRegistry.append(Integer.class, InputStream.class, new ResourceLoader.StreamFactory());
        modelLoaderRegistry.append(Integer.class, ParcelFileDescriptor.class,
                new ResourceLoader.FileDescriptorFactory());
        modelLoaderRegistry.append(String.class, InputStream.class, new StringLoader.StreamFactory());
        modelLoaderRegistry.append(String.class, ParcelFileDescriptor.class, new StringLoader.FileDescriptorFactory());
        modelLoaderRegistry.append(Uri.class, InputStream.class, new HttpUriLoader.Factory());
        modelLoaderRegistry.append(Uri.class, InputStream.class, new AssetUriLoader.StreamFactory());
        modelLoaderRegistry.append(Uri.class, ParcelFileDescriptor.class, new AssetUriLoader.FileDescriptorFactory());
        modelLoaderRegistry.append(Uri.class, InputStream.class, new MediaStoreImageThumbLoader.Factory());
        modelLoaderRegistry.append(Uri.class, InputStream.class, new MediaStoreVideoThumbLoader.Factory());
        modelLoaderRegistry.append(Uri.class, InputStream.class, new UriLoader.StreamFactory());
        modelLoaderRegistry.append(Uri.class, ParcelFileDescriptor.class, new UriLoader.FileDescriptorFactory());
        modelLoaderRegistry.append(URL.class, InputStream.class, new UrlLoader.StreamFactory());
        modelLoaderRegistry.append(GlideUrl.class, InputStream.class, new HttpGlideUrlLoader.Factory());
        modelLoaderRegistry.append(byte[].class, InputStream.class, new ByteArrayLoader.StreamFactory());

        TranscoderRegistry transcoderRegistry = new TranscoderRegistry();
        transcoderRegistry.register(Bitmap.class, BitmapDrawable.class, new BitmapDrawableTranscoder(context
                .getResources(), bitmapPool));
        transcoderRegistry.register(Bitmap.class, byte[].class, new BitmapBytesTranscoder());
        transcoderRegistry.register(GifDrawable.class, byte[].class, new GifDrawableBytesTranscoder());

        ImageViewTargetFactory imageViewTargetFactory = new ImageViewTargetFactory();
        glideContext = new GlideContext(context, modelLoaderRegistry, encoderRegistry, decoderRegistry,
                resourceEncoderRegistry, dataRewinderRegistry, transcoderRegistry, imageViewTargetFactory, engine,
                this);
    }

    /**
     * Returns the {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} used to temporarily store
     * {@link android.graphics.Bitmap}s so they can be reused to avoid garbage collections.
     *
     * <p>
     *     Note - Using this pool directly can lead to undefined behavior and strange drawing errors. Any
     *     {@link android.graphics.Bitmap} added to the pool must not be currently in use in any other part of the
     *     application. Any {@link android.graphics.Bitmap} added to the pool must be removed from the pool before it
     *     is added a second time.
     * </p>
     *
     * <p>
     *     Note - To make effective use of the pool, any {@link android.graphics.Bitmap} removed from the pool must
     *     eventually be re-added. Otherwise the pool will eventually empty and will not serve any useful purpose.
     * </p>
     *
     * <p>
     *     The primary reason this object is exposed is for use in custom
     *     {@link com.bumptech.glide.load.ResourceDecoder}s and {@link com.bumptech.glide.load.Transformation}s. Use
     *     outside of these classes is not generally recommended.
     * </p>
     */
    public BitmapPool getBitmapPool() {
        return bitmapPool;
    }

    GlideContext getGlideContext() {
        return glideContext;
    }

    /**
     * Pre-fills the {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} using the given sizes.
     *
     * <p>
     *   Enough Bitmaps are added to completely fill the pool, so most or all of the Bitmaps currently in the pool will
     *   be evicted. Bitmaps are allocated according to the weights of the given sizes, where each size gets
     *   (weight / prefillWeightSum) percent of the pool to fill.
     * </p>
     *
     * <p>
     *     Note - Pre-filling is done asynchronously using and {@link android.os.MessageQueue.IdleHandler}. Any
     *     currently running pre-fill will be cancelled and replaced by a call to this method.
     * </p>
     *
     * <p>
     *     This method should be used with caution, overly aggressive pre-filling is substantially worse than not
     *     pre-filling at all. Pre-filling should only be started in onCreate to avoid constantly clearing and
     *     re-filling the {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool}. Rotation should be carefully
     *     considered as well. It may be worth calling this method only when no saved instance state exists so that
     *     pre-filling only happens when the Activity is first created, rather than on every rotation.
     * </p>
     *
     * @param bitmapAttributeBuilders The list of
     *     {@link com.bumptech.glide.load.engine.prefill.PreFillType.Builder Builders} representing
     *     individual sizes and configurations of {@link android.graphics.Bitmap}s to be pre-filled.
     */
    public void preFillBitmapPool(PreFillType.Builder... bitmapAttributeBuilders) {
        bitmapPreFiller.preFill(bitmapAttributeBuilders);
    }

    /**
     * Clears as much memory as possible.
     *
     * @see android.content.ComponentCallbacks#onLowMemory()
     * @see android.content.ComponentCallbacks2#onLowMemory()
     */
    public void clearMemory() {
        bitmapPool.clearMemory();
        memoryCache.clearMemory();
    }

    /**
     * Clears some memory with the exact amount depending on the given level.
     *
     * @see android.content.ComponentCallbacks2#onTrimMemory(int)
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
    public static void clear(Target<?> target) {
        Util.assertMainThread();
        Request request = target.getRequest();
        if (request != null) {
            request.clear();
        }
    }

    /**
     * Cancel any pending loads Glide may have for the target and free any resources that may have been loaded into
     * the target so they may be reused.
     *
     * @param target The target to cancel loads for.
     */
    public static void clear(FutureTarget<?> target) {
        target.clear();
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
        Target<?> viewTarget = new ClearTarget(view);
        clear(viewTarget);
    }

    /**
     * Use the given factory to build a {@link ModelLoader} for models of the given class. Generally the best use of
     * this method is to replace one of the default factories or add an implementation for other similar low level
     * models. Any factory replaced by the given factory will have its {@link ModelLoaderFactory#teardown()}} method
     * called.
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
     * @param modelClass The model class.
     * @param resourceClass The resource class the model loader will translate the model type into.
     * @param factory The factory to use.
     * @param <T> The type of the model.
     * @param <Y> the type of the resource.
     */
    public <Model, Data> void append(Class<Model> modelClass, Class<Data> dataClass,
            ModelLoaderFactory<Model, Data> factory) {
        modelLoaderRegistry.append(modelClass, dataClass, factory);
    }

    public <Model, Data> void prepend(Class<Model> modelClass, Class<Data> dataClass,
            ModelLoaderFactory<Model, Data> factory) {
        modelLoaderRegistry.prepend(modelClass, dataClass, factory);
    }

    public <Model, Data> void replace(Class<Model> modelClass, Class<Data> dataClass,
            ModelLoaderFactory<Model, Data> factory) {
        modelLoaderRegistry.replace(modelClass, dataClass, factory);
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
    public <Model, Data> void remove(Class<Model> modelClass, Class<Data> dataClass) {
        modelLoaderRegistry.remove(modelClass, dataClass);
    }

    /**
     * Begin a load with Glide by passing in a context.
     *
     * <p>
     *     Any requests started using a context will only have the application level options applied and will not be
     *     started or stopped based on lifecycle events. In general, loads should be started at the level the result
     *     will be used in. If the resource will be used in a view in a child fragment,
     *     the load should be started with {@link #with(android.app.Fragment)}} using that child fragment. Similarly,
     *     if the resource will be used in a view in the parent fragment, the load should be started with
     *     {@link #with(android.app.Fragment)} using the parent fragment. In the same vein, if the resource will be used
     *     in a view in an activity, the load should be started with {@link #with(android.app.Activity)}}.
     * </p>
     *
     * <p>
     *     This method is appropriate for resources that will be used outside of the normal fragment or activity
     *     lifecycle (For example in services, or for notification thumbnails).
     * </p>
     *
     * @see #with(android.app.Activity)
     * @see #with(android.app.Fragment)
     * @see #with(android.support.v4.app.Fragment)
     * @see #with(android.support.v4.app.FragmentActivity)
     *
     * @param context Any context, will not be retained.
     * @return A RequestManager for the top level application that can be used to start a load.
     */
    public static RequestManager with(Context context) {
        RequestManagerRetriever retriever = RequestManagerRetriever.get();
        return retriever.get(context);
    }

    /**
     * Begin a load with Glide that will be tied to the given {@link android.app.Activity}'s lifecycle and that uses the
     * given {@link Activity}'s default options.
     *
     * @param activity The activity to use.
     * @return A RequestManager for the given activity that can be used to start a load.
     */
    public static RequestManager with(Activity activity) {
        RequestManagerRetriever retriever = RequestManagerRetriever.get();
        return retriever.get(activity);
    }

    /**
     * Begin a load with Glide that will tied to the give {@link android.support.v4.app.FragmentActivity}'s lifecycle
     * and that uses the given {@link android.support.v4.app.FragmentActivity}'s default options.
     *
     * @param activity The activity to use.
     * @return A RequestManager for the given FragmentActivity that can be used to start a load.
     */
    public static RequestManager with(FragmentActivity activity) {
        RequestManagerRetriever retriever = RequestManagerRetriever.get();
        return retriever.get(activity);
    }

    /**
     * Begin a load with Glide that will be tied to the given {@link android.app.Fragment}'s lifecycle and that uses
     * the given {@link android.app.Fragment}'s default options.
     *
     * @param fragment The fragment to use.
     * @return A RequestManager for the given Fragment that can be used to start a load.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static RequestManager with(android.app.Fragment fragment) {
        RequestManagerRetriever retriever = RequestManagerRetriever.get();
        return retriever.get(fragment);
    }

    /**
     * Begin a load with Glide that will be tied to the given {@link android.support.v4.app.Fragment}'s lifecycle and
     * that uses the given {@link android.support.v4.app.Fragment}'s default options.
     *
     * @param fragment The fragment to use.
     * @return A RequestManager for the given Fragment that can be used to start a load.
     */
    public static RequestManager with(Fragment fragment) {
        RequestManagerRetriever retriever = RequestManagerRetriever.get();
        return retriever.get(fragment);
    }

    @Override
    public void onTrimMemory(int level) {
        trimMemory(level);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // Do nothing.
    }

    @Override
    public void onLowMemory() {
        clearMemory();
    }

    private static class ClearTarget extends ViewTarget<View, Object> {
        public ClearTarget(View view) {
            super(view);
        }

        @Override
        public void onResourceReady(Object resource, GlideAnimation<? super Object> glideAnimation) {
            // Do nothing.
        }
    }
}
