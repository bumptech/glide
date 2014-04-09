/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.resize;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.resize.bitmap_recycle.BitmapReferenceCounter;
import com.bumptech.glide.resize.bitmap_recycle.BitmapReferenceCounterAdapter;
import com.bumptech.glide.resize.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.resize.bitmap_recycle.SerialBitmapReferenceCounter;
import com.bumptech.glide.resize.cache.DiskCache;
import com.bumptech.glide.resize.cache.DiskCacheAdapter;
import com.bumptech.glide.resize.cache.DiskLruCacheWrapper;
import com.bumptech.glide.resize.cache.LruMemoryCache;
import com.bumptech.glide.resize.cache.MemoryCache;
import com.bumptech.glide.resize.cache.MemoryCacheAdapter;
import com.bumptech.glide.resize.load.Downsampler;
import com.bumptech.glide.resize.load.ImageResizer;
import com.bumptech.glide.resize.load.Transformation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

/**
 * A class to coordinate image loading, resizing, recycling, and caching. Depending on the provided options and the
 * sdk version, uses a combination of an LRU disk cache and an LRU hard memory cache to try to reduce the number of
 * load and resize operations performed and to maximize the number of times Bitmaps are recycled as opposed to
 * allocated. If no options are given defaults to using both a memory and a disk cache and to recycling bitmaps if
 * possible.
 *
 * <p>
 * Note that Bitmap recycling is only available on Honeycomb and up.
 * </p>
 */
public class ImageManager {
    private static final String TAG = "ImageManager";
    private static final String DEFAULT_DISK_CACHE_DIR = "image_manager_disk_cache";
    private static final int DEFAULT_DISK_CACHE_SIZE = 250 * 1024 * 1024;
    private static final int DEFAULT_BITMAP_COMPRESS_QUALITY = 90;
    private static final float MEMORY_SIZE_RATIO = 1f/10f;
    public static final boolean CAN_RECYCLE = Build.VERSION.SDK_INT >= 11;

    private final BitmapReferenceCounter bitmapReferenceCounter;
    private final int bitmapCompressQuality;
    private final BitmapPool bitmapPool;
    private final Map<String, ImageManagerJob> jobs = new HashMap<String, ImageManagerJob>();
    private final Bitmap.CompressFormat bitmapCompressFormat;
    private boolean shutdown = false;

    private final Handler mainHandler = new Handler();
    private final Handler bgHandler;
    private final ExecutorService executor;
    private final MemoryCache memoryCache;
    private final ImageResizer resizer;
    private final DiskCache diskCache;
    private final SafeKeyGenerator safeKeyGenerator = new SafeKeyGenerator();

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

    @SuppressWarnings("unused")
    public static class Builder {
        private final Context context;

        private ExecutorService resizeService = null;
        private MemoryCache memoryCache = null;
        private DiskCache diskCache = null;

        private Bitmap.CompressFormat bitmapCompressFormat = null;
        private boolean recycleBitmaps = CAN_RECYCLE;

        @Deprecated
        public BitmapFactory.Options decodeBitmapOptions = ImageResizer.getDefaultOptions();

        private BitmapPool bitmapPool;
        private BitmapReferenceCounter bitmapReferenceCounter;
        private int bitmapCompressQuality = DEFAULT_BITMAP_COMPRESS_QUALITY;

        /**
         * Create a new builder. No options are required. By default will create an lru memory cache, an lru disk
         * cache, and will recycle bitmaps if the device sdk version allows it.
         *
         * @param context Any context (will not be retained after build)
         */
        public Builder(Context context) {
            this.context = context;
            if (!CAN_RECYCLE) {
                bitmapPool = new BitmapPoolAdapter();
            }
        }

        /**
         * Builds an ImageManager. Any defaults that haven't been set will be set
         *
         * @return A new ImageManager
         */
        public ImageManager build() {
            setDefaults();

            return new ImageManager(this);
        }

        /**
         * Sets the service that will be used to load and resize images not yet in the disk cache.
         *
         * Defaults to a fixed thread pool with the number of threads equal to the number of available processors
         * where every thread is run at min priority.
         *
         * @param resizeService The executor service to use to resize images
         * @return This Builder
         */
        public Builder setResizeService(ExecutorService resizeService) {
            this.resizeService = resizeService;
            return this;
        }

        /**
         * Sets the format that will be used to write all bitmaps to disk in the disk cache (if one is present). By
         * default bitmaps without transparency are written as JPEGs for the fastest possible decodes and bitmaps with
         * transparency are written as PNGs to maximize quality. This will override the format used for all bitmaps,
         * regardless of whether or not they contain transparency.
         *
         * @see Bitmap#compress(android.graphics.Bitmap.CompressFormat, int, java.io.OutputStream)
         *
         *
         * @param bitmapCompressFormat The format to pass to
         *  {@link Bitmap#compress(android.graphics.Bitmap.CompressFormat, int, java.io.OutputStream)} when saving
         *  to the disk cache
         * @return This Builder
         */
        public Builder setBitmapCompressFormat(Bitmap.CompressFormat bitmapCompressFormat) {
            this.bitmapCompressFormat = bitmapCompressFormat;
            return this;
        }

        /**
         * Set the compression quality for Bitmaps when writing them out to the disk cache.
         *
         * @see Bitmap#compress(android.graphics.Bitmap.CompressFormat, int, java.io.OutputStream)
         * @see #setBitmapCompressFormat(android.graphics.Bitmap.CompressFormat)
         *
         * <Note>
         *     This will only apply to bitmaps saved to the disk cache as JPEGs (bitmaps with the RGB_565 config)
         * </Note>
         *
         * @param quality Hint for compression in range 0-100 with 0 being lowest and 100 being highest quality. Will
         *                only be applied for certain lossy compression formats
         * @return This Builder
         */
        public Builder setBitmapCompressQuality(int quality) {
            if (quality < 0) {
                throw new IllegalArgumentException("Bitmap compression quality must be >= 0");
            }
            this.bitmapCompressQuality = quality;
            return this;
        }

        /**
         * Set the implementation of a {@link BitmapPool} to use to store and retrieve recycled bitmaps based on their
         * width and height. Should be thread safe and size limited in some way to avoid OOM exceptions.
         *
         * @param bitmapPool The BitmapPool implementation to use
         * @return This Builder
         */
        public Builder setBitmapPool(BitmapPool bitmapPool) {
            if (CAN_RECYCLE) {
                this.bitmapPool = bitmapPool;
            }
            return this;
        }

        /**
         * Call to prevent the ImageManager from recycling bitmaps.
         *
         * @return This Builder
         */
        public Builder disableBitmapRecycling() {
            recycleBitmaps = false;
            return this;
        }

        /**
         * Set the memory cache implementation. See also
         * {@link com.bumptech.glide.resize.ImageManager.Builder#disableMemoryCache()}
         *
         * @param memoryCache The memory cache implementation to use
         * @return This Builder
         */
        public Builder setMemoryCache(MemoryCache memoryCache) {
            this.memoryCache = memoryCache;
            return this;
        }

        /**
         * Call to prevent the ImageManager from using a memory cache.
         *
         * @return This Builder
         */
        public Builder disableMemoryCache() {
            return setMemoryCache(new MemoryCacheAdapter());
        }

        /**
         * Set the disk cache implementation. See also
         *  {@link com.bumptech.glide.resize.ImageManager.Builder#disableDiskCache()}
         *
         * @param diskCache The disk cache implementation to use
         * @return This Builder
         */
        public Builder setDiskCache(DiskCache diskCache) {
            this.diskCache = diskCache;
            return this;
        }

        /**
         * Call to prevent the ImageManager from using a disk cache
         * @return This Builder
         */
        public Builder disableDiskCache() {
            return setDiskCache(new DiskCacheAdapter());
        }

        private void setDefaults() {
            if (resizeService == null) {
                final int numThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
                resizeService = Executors.newFixedThreadPool(numThreads, new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        final Thread result = new Thread(runnable);
                        result.setPriority(THREAD_PRIORITY_BACKGROUND);
                        return result;
                    }
                });
            }
            final int safeCacheSize = getSafeMemoryCacheSize(context);
            final boolean isLowMemoryDevice = isLowMemoryDevice(context);

            if (memoryCache == null) {
                // On low ram devices we double the default bitmap pool size by default so we decrease
                // the default memory cache size here to compensate.
                memoryCache = new LruMemoryCache(
                        !isLowMemoryDevice && recycleBitmaps ? safeCacheSize / 2 : safeCacheSize);
            }

            if (diskCache == null) {
                File cacheDir = getPhotoCacheDir(context);
                if (cacheDir != null) {
                    try {
                        diskCache = DiskLruCacheWrapper.get(cacheDir, DEFAULT_DISK_CACHE_SIZE);
                    } catch (IOException e) {
                        //this is probably a corrupt or full sd card, so default to not using a disk cache
                        e.printStackTrace();
                    }
                }

                if (diskCache == null) {
                    diskCache = new DiskCacheAdapter();
                }
            }

            if (!recycleBitmaps) {
                bitmapPool = new BitmapPoolAdapter();
                bitmapReferenceCounter = new BitmapReferenceCounterAdapter();
            } else {
                if (bitmapPool == null) {
                    bitmapPool = new LruBitmapPool(
                            isLowMemoryDevice ? safeCacheSize : 2 * safeCacheSize);
                }
                bitmapReferenceCounter = new SerialBitmapReferenceCounter(bitmapPool);
            }
        }
    }

    @TargetApi(19)
    private static boolean isLowMemoryDevice(Context context) {
        final ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return Build.VERSION.SDK_INT < 11 ||
                (Build.VERSION.SDK_INT >= 19 && activityManager.isLowRamDevice());
    }

    private ImageManager(Builder builder) {
        HandlerThread bgThread = new HandlerThread("image_manager_thread", THREAD_PRIORITY_BACKGROUND);
        bgThread.start();
        bgHandler = new Handler(bgThread.getLooper());
        executor = builder.resizeService;
        bitmapCompressFormat = builder.bitmapCompressFormat;
        bitmapCompressQuality = builder.bitmapCompressQuality;
        memoryCache = builder.memoryCache;
        diskCache = builder.diskCache;
        bitmapReferenceCounter = builder.bitmapReferenceCounter;
        bitmapPool = builder.bitmapPool;
        resizer = new ImageResizer(builder.bitmapPool, builder.decodeBitmapOptions);
        memoryCache.setImageRemovedListener(new MemoryCache.ImageRemovedListener() {
            @Override
            public void onImageRemoved(Bitmap removed) {
                releaseBitmap(removed);
            }
        });
    }

    /**
     * Get the {@link BitmapPool} this ImageManager is using. If Bitmap recycling is not supported, an
     * {@link BitmapPoolAdapter} will be returned. For the pool to be useful you must return a bitmap to the pool for
     * every bitmap you obtain from the pool.
     *
     * <p>
     *     Note the BitmapPool api is likely to change in the near future to support some new features released in
     *     KitKat.
     * </p>
     *
     * @return The bitmap pool.
     */
    public BitmapPool getBitmapPool() {
        return bitmapPool;
    }

    /**
     * Load an image
     *
     * @param id A unique id for this image (it may include the width and height but is not required to)
     * @param streamLoader An object that can fetch the image for the given id, width, and height if it is not cached
     * @param transformation A transformation to apply to the image
     * @param downsampler A downsampler to load the image from disk
     * @param width The desired width of the final image
     * @param height The desired height of the final image
     * @param cb A callback to call when the image is ready
     * @return An {@link ImageManagerJob} that must be retained while the job is still relevant and that can be used
     *          to cancel a job if the image is no longer needed
     */
    public LoadToken getImage(String id, StreamLoader streamLoader, Transformation transformation,
                              Downsampler downsampler, int width, int height, LoadedCallback cb) {
        if (shutdown) return null;

        final String key = safeKeyGenerator.getSafeKey(id, transformation, downsampler, width, height);
        LoadToken result = null;
        if (!returnFromCache(key, cb)) {
            ImageManagerJob job = jobs.get(key);
            if (job == null) {
                ImageManagerRunner runner = new ImageManagerRunner(key, streamLoader, transformation, downsampler,
                        width, height);
                job = new ImageManagerJob(runner, key);
                jobs.put(key, job);
                job.addCallback(cb);
                runner.execute();
            } else {
                job.addCallback(cb);
            }
            result = new LoadToken(cb, job);
        }
        return result;
    }

    /**
     * Notify the ImageManager that a Bitmap it loaded is no longer being used and decrement the reference counter for
     * that Bitmap. This will cause an exception if acquire was not called first, or if each call to release does not
     * come after a call to acquire. If the reference count drops to zero, places the Bitmap into a queue to be
     * recycled. Does nothing if recycling is disabled or impossible.
     *
     * @param b The releasedBitmap
     */
    public void releaseBitmap(final Bitmap b) {
        bitmapReferenceCounter.releaseBitmap(b);
    }

    public void clearMemory() {
        memoryCache.clearMemory();
        bitmapPool.clearMemory();
    }

    public void trimMemory(int level) {
        memoryCache.trimMemory(level);
        bitmapPool.trimMemory(level);
    }

    /**
     * Shuts down all of the background threads used by the ImageManager including the executor service
     */
    @SuppressWarnings("unused")
    public void shutdown() {
        shutdown = true;
        executor.shutdown();
        bgHandler.getLooper().quit();
    }

    private boolean returnFromCache(String key, LoadedCallback cb) {
        Bitmap inCache = memoryCache.get(key);
        boolean found = inCache != null;
        if (found) {
            bitmapReferenceCounter.acquireBitmap(inCache);
            cb.onLoadCompleted(inCache);
        }
        return found;
    }

    /**
     * A class for tracking a particular job in the {@link ImageManager}. Cancel does not guarantee that the
     * job will not finish, but rather is a best effort attempt.
     */
    private class ImageManagerJob {
        private final ImageManagerRunner runner;
        private final String key;
        private final List<LoadedCallback> cbs = new ArrayList<LoadedCallback>();

        public ImageManagerJob(ImageManagerRunner runner, String key) {
            this.runner = runner;
            this.key = key;
        }

        public void addCallback(LoadedCallback cb) {
            cbs.add(cb);
        }

        /**
         * Try to cancel the job. Does not guarantee that the job will not finish.
         */
        public void cancel(LoadedCallback cb) {
            cbs.remove(cb);
            if (cbs.size() == 0) {
                // Note: this is potentially dangerous. The runner asynchronously asks our jobs map for a job
                // matching our key after posting a runnable to the main thread and as a result, the job it gets back
                // may not be this job. We protect against this for cancellation by not delivering failures from
                // cancelled runners, so new jobs will not receive errors from cancelled jobs. However, new jobs may
                // receive results from old runners if the old runner was cancelled, but completed successfully anyway
                // because it received the cancellation too late.
                runner.cancel();
                jobs.remove(key);
            }
        }

        public void onLoadComplete(Bitmap result) {
            for (LoadedCallback cb : cbs) {
                bitmapReferenceCounter.acquireBitmap(result);
                cb.onLoadCompleted(result);
            }
            jobs.remove(key);
        }

        public void onLoadFailed(Exception e) {
            for (LoadedCallback cb : cbs) {
                cb.onLoadFailed(e);
            }
            jobs.remove(key);
        }
    }

    private void putInDiskCache(String key, final Bitmap bitmap) {
        diskCache.put(key, new DiskCache.Writer() {
            @Override
            public void write(OutputStream os) {
                Bitmap.CompressFormat compressFormat = getCompressFormat(bitmap);
                bitmap.compress(compressFormat, bitmapCompressQuality, os);
            }
        });
    }

    private Bitmap.CompressFormat getCompressFormat(Bitmap bitmap) {
        final Bitmap.CompressFormat format;
        if (bitmapCompressFormat != null) {
            format = bitmapCompressFormat;
        } else {
            if (bitmap.getConfig() == Bitmap.Config.RGB_565 || !bitmap.hasAlpha()) {
                format = Bitmap.CompressFormat.JPEG;
            } else {
                format = Bitmap.CompressFormat.PNG;
            }
        }
        return format;
    }

    private void putInMemoryCache(String key, final Bitmap bitmap) {
        final boolean inCache;
        inCache = memoryCache.contains(key);
        if (!inCache) {
            bitmapReferenceCounter.acquireBitmap(bitmap);
            memoryCache.put(key, bitmap);
        }
    }

    private class ImageManagerRunner implements Runnable {
        public final String key;
        public final int width;
        public final int height;
        private final StreamLoader streamLoader;
        private final Transformation transformation;
        private final Downsampler downsampler;

        private volatile Future<?> future;
        private volatile boolean isCancelled = false;

        public ImageManagerRunner(String key, StreamLoader sl, Transformation t, Downsampler d, int width, int height) {
            this.key = key;
            this.height = height;
            this.width = width;

            this.streamLoader = sl;
            this.transformation = t;
            this.downsampler = d;
        }

        private void execute() {
            bgHandler.post(this);
        }

        public void cancel() {
            if (isCancelled) {
                return;
            }
            isCancelled = true;

            bgHandler.removeCallbacks(this);

            final Future current = future;
            if (current != null) {
                current.cancel(false);
            }

            if (streamLoader != null) {
                streamLoader.cancel();
            }
        }

        @Override
        public void run() {
            Bitmap result = null;
            try {
                result = getFromDiskCache(key);
            } catch (Exception e) {
                handleException(e);
            }

            if (result == null) {
                try {
                    resizeWithPool();
                } catch (Exception e) {
                    handleException(e);
                }
            } else {
                finishResize(result, true);
            }
        }

        private Bitmap getFromDiskCache(String key) {
            Bitmap result = null;
            final InputStream is = diskCache.get(key);
            if (is != null) {
                result = resizer.load(is, width, height, Downsampler.NONE);
                if (result == null) {
                    diskCache.delete(key); //the image must have been corrupted
                }
            }
            return result;
        }

        private void resizeWithPool() {
            future = executor.submit(new Runnable() {
                @Override
                public void run() {

                    streamLoader.loadStream(new StreamLoader.StreamReadyCallback() {
                        @Override
                        public void onStreamReady(final InputStream is) {
                            if (isCancelled) {
                                return;
                            }

                            //this callback might be called on some other thread,
                            //we want to do resizing on our thread, especially if we're called
                            //back on the main thread, so we will resubmit
                            future = executor.submit(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        final Bitmap result = resizeIfNotFound(is, downsampler, transformation);
                                        finishResize(result, false);
                                    } catch (Exception e) {
                                        handleException(e);
                                    }
                                }
                            });
                        }

                        @Override
                        public void onException(Exception e) {
                            handleException(e);
                        }
                    });
                }
            });
        }

        private Bitmap resizeIfNotFound(InputStream is, Downsampler downsampler, Transformation transformation) {
            return resizer.load(is, width, height, downsampler, transformation);
        }

        private void finishResize(final Bitmap result, boolean isInDiskCache) {
            if (result != null) {
                if (!isInDiskCache) {
                    putInDiskCache(key, result);
                }

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Acquire the bitmap for this runnable until we've finished notifying
                        // all consumers. This prevents the bitmap from being put in the bitmap pool
                        // before all consumers have a change to acquire the bitmap if one of the first
                        // consumers (usually the memory cache) synchronously releases the bitmap.
                        bitmapReferenceCounter.acquireBitmap(result);
                        putInMemoryCache(key, result);
                        final ImageManagerJob job = jobs.get(key);
                        if (job != null) {
                            job.onLoadComplete(result);
                        }
                        // All consumers have had their chance, it's now safe to release the
                        // bitmap.
                        bitmapReferenceCounter.releaseBitmap(result);
                    }
                });
            } else {
                handleException(null);
            }
        }

        private void handleException(final Exception e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Exception loading image", e);
            }
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (isCancelled) {
                        return;
                    }

                    final ImageManagerJob job = jobs.get(key);
                    if (job != null) {
                        job.onLoadFailed(e);
                    }
                }
            });
        }
    }

    public static class LoadToken {
        private final ImageManagerJob job;
        private final LoadedCallback cb;

        public LoadToken(LoadedCallback cb, ImageManagerJob job) {
            this.cb = cb;
            this.job = job;
        }

        public void cancel() {
            job.cancel(cb);
        }
    }
}
