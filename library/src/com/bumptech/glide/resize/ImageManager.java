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
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.resize.bitmap_recycle.BitmapReferenceCounter;
import com.bumptech.glide.resize.bitmap_recycle.BitmapReferenceCounterAdapter;
import com.bumptech.glide.resize.bitmap_recycle.SerialBitmapReferenceCounter;
import com.bumptech.glide.resize.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.resize.cache.DiskCache;
import com.bumptech.glide.resize.cache.DiskCacheAdapter;
import com.bumptech.glide.resize.cache.DiskLruCacheWrapper;
import com.bumptech.glide.resize.cache.LruMemoryCache;
import com.bumptech.glide.resize.cache.MemoryCache;
import com.bumptech.glide.resize.cache.MemoryCacheAdapter;
import com.bumptech.glide.resize.load.Downsampler;
import com.bumptech.glide.resize.load.ImageResizer;
import com.bumptech.glide.resize.load.Transformation;
import com.bumptech.glide.util.Log;
import com.bumptech.glide.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * A class to coordinate image loading, resizing, recycling, and caching. Depending on the provided options and the
 * sdk version, uses a combination of an LRU disk cache and an LRU hard memory cache to try to reduce the number of
 * load and resize operations performed and to maximize the number of times Bitmaps are recycled as opposed to
 * allocated. If no options are given defaults to using both a memory and a disk cache and to recycling bitmaps if possible.
 *
 * <p>
 * Note that Bitmap recycling is only available on Honeycomb and up.
 * </p>
 */
public class ImageManager {
    private static final String DEFAULT_DISK_CACHE_DIR = "image_manager_disk_cache";
    private static final int DEFAULT_DISK_CACHE_SIZE = 30 * 1024 * 1024;
    private static final int DEFAULT_BITMAP_COMPRESS_QUALITY = 90;
    private static final float MEMORY_SIZE_RATIO = 1f/10f;
    public static final boolean CAN_RECYCLE = Build.VERSION.SDK_INT >= 11;

    private final BitmapReferenceCounter bitmapReferenceCounter;
    private final int bitmapCompressQuality;
    private boolean shutdown = false;

    private final Handler mainHandler = new Handler();
    private final Handler bgHandler;
    private final ExecutorService executor;
    private final MemoryCache memoryCache;
    private final ImageResizer resizer;
    private final DiskCache diskCache;
    private final Bitmap.CompressFormat bitmapCompressFormat;

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
        File cacheDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                !isExternalStorageRemovable()) {
            //seems like this can still be null even if the above are true
            cacheDir = context.getExternalCacheDir();
        }

        if (cacheDir == null) {
            cacheDir = context.getCacheDir();
        }

        if (cacheDir != null) {
            File result = new File(cacheDir, cacheName);
            result.mkdirs();
            return result;
        }
        Log.d("IM: default disk cache dir is null");
        return null;
    }

    @TargetApi(9)
    private static boolean isExternalStorageRemovable() {
        return Build.VERSION.SDK_INT < 9 || Environment.isExternalStorageRemovable();
    }

    @SuppressWarnings("unused")
    public static class Builder {
        private final Context context;

        private ExecutorService resizeService = null;
        private MemoryCache memoryCache = null;
        private DiskCache diskCache = null;

        private Bitmap.CompressFormat bitmapCompressFormat = Bitmap.CompressFormat.JPEG;
        private boolean recycleBitmaps = CAN_RECYCLE;

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
         * Sets the format that will be used to write bitmaps to disk in the disk cache (if one is present). Defaults
         * to JPEG. Set to PNG if you need transparency
         *
         * @see Bitmap#compress(android.graphics.Bitmap.CompressFormat, int, java.io.OutputStream)
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
                resizeService = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors()), new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        final Thread result = new Thread(runnable);
                        result.setPriority(Thread.MIN_PRIORITY);
                        return result;
                    }
                });
            }

            if (memoryCache == null) {
                memoryCache = new LruMemoryCache(getSafeMemoryCacheSize(context));
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

                diskCache = new DiskCacheAdapter();
            }

            if (!recycleBitmaps) {
                bitmapPool = new BitmapPoolAdapter();
                bitmapReferenceCounter = new BitmapReferenceCounterAdapter();
            } else {
                if (bitmapPool == null) {
                    bitmapPool = new LruBitmapPool(getSafeMemoryCacheSize(context));
                }
                bitmapReferenceCounter = new SerialBitmapReferenceCounter(bitmapPool);
            }
        }
    }

    private ImageManager(Builder builder) {
        HandlerThread bgThread = new HandlerThread("bg_thread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        bgThread.start();
        bgHandler = new Handler(bgThread.getLooper());
        executor = builder.resizeService;
        bitmapCompressFormat = builder.bitmapCompressFormat;
        bitmapCompressQuality = builder.bitmapCompressQuality;
        memoryCache = builder.memoryCache;
        diskCache = builder.diskCache;
        bitmapReferenceCounter = builder.bitmapReferenceCounter;
        resizer = new ImageResizer(builder.bitmapPool, builder.decodeBitmapOptions);

        memoryCache.setImageRemovedListener(new MemoryCache.ImageRemovedListener() {
            @Override
            public void onImageRemoved(Bitmap removed) {
                releaseBitmap(removed);
            }
        });
    }

    public Object getImage(String id, StreamLoader streamLoader, Transformation transformation, Downsampler downsampler, int width, int height, LoadedCallback cb) {
        if (shutdown) return null;

        final String key = getKey(id, transformation.getId(), downsampler, width, height);

        ImageManagerJob job = null;
        if (!returnFromCache(key, cb)) {
            job = new ImageManagerJob();
            job.execute(key, streamLoader, transformation, downsampler, width, height, cb);
        }
        return job;
    }

    /**
     * Notify the ImageManager that a bitmap it loaded is not going to be displayed and can go into a queue to be
     * reused. Does nothing if recycling is disabled or impossible.
     *
     * @param b The rejected Bitmap
     */
    public void rejectBitmap(final Bitmap b) {
        bitmapReferenceCounter.rejectBitmap(b);
    }

    /**
     * Notify the ImageManager that a Bitmap it loaded is going to be used and increment the reference counter for that
     * Bitmap. Though it won't cause a memory leak, we expect releaseBitmap to be called for this Bitmap at some point.
     * If release is not called, then we will never be able to recycle the Bitmap. Does nothing if recycling is disabled
     * or impossible.
     *
     * @param b The acquired Bitmap
     */
    public void acquireBitmap(final Bitmap b) {
        bitmapReferenceCounter.acquireBitmap(b);
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

    /**
     * Cancel the task represented by the given token. If token is null this call will be ignored.
     *
     * @param token The token returned by the ImageManager in a get call
     */
    public void cancelTask(Object token) {
        if (token != null) {
            ImageManagerJob job = (ImageManagerJob) token;
            job.cancel();
        }
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
            cb.onLoadCompleted(inCache);
        }
        return found;
    }

    private class ImageManagerJob<T> implements Runnable {
        private LoadedCallback cb;
        private Future future = null;
        private volatile boolean cancelled = false;
        private Downsampler downsampler;
        private int width;
        private int height;
        private String key;
        private StreamLoader streamLoader = null;
        private Transformation transformation;

        public void execute(String key, StreamLoader streamLoader, Transformation transformation, Downsampler downsampler, int width, int height, LoadedCallback cb) {
            this.key = key;
            this.streamLoader = streamLoader;
            this.transformation = transformation;
            this.cb = cb;
            this.width = width;
            this.height = height;
            this.downsampler = downsampler;
            bgHandler.post(this);
        }

        public void cancel() {
            cancelled = true;
            bgHandler.removeCallbacks(this);
            if (future != null) {
                future.cancel(false);
            }
            if (streamLoader != null) {
                streamLoader.cancel();
            }
        }

        @Override
        public void run() {
            Bitmap result = getFromDiskCache(key);

            if (result == null) {
                try {
                    //in almost every case exception will be because of race after calling shutdown. Not much we can do
                    //either way
                    resizeWithPool();
                } catch (Exception e) {
                    handleException(e);
                }
            } else {
                finishResize(result, true);
            }
        }

        private void resizeWithPool() throws RejectedExecutionException {
            future = executor.submit(new Runnable() {
                @Override
                public void run() {
                    streamLoader.loadStream(new StreamLoader.StreamReadyCallback() {
                        @Override
                        public void onStreamReady(final InputStream is) {
                            if (cancelled) {
                                return;
                            }

                            //this call back might be called on some other thread,
                            //we want to do resizing on our thread, especially if we're called
                            //back on the main thread, so we will resubmit
                            future = executor.submit(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        final Bitmap result = resizeIfNotFound(is);
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

        private void finishResize(final Bitmap result, boolean isInDiskCache) {
            if (result != null) {
                if (!isInDiskCache) {
                    putInDiskCache(key, result);
                }

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        bitmapReferenceCounter.initBitmap(result);
                        putInMemoryCache(key, result);
                        cb.onLoadCompleted(result);
                    }
                });
            } else {
                handleException(null);
            }
        }

        private void handleException(final Exception e) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    cb.onLoadFailed(e);
                }
            });
        }

        private Bitmap resizeIfNotFound(InputStream is) throws IOException {
            return resizer.load(is, width, height, downsampler, transformation);
        }
    }

    private Bitmap getFromDiskCache(String key) {
        Bitmap result = null;
        final InputStream is = diskCache.get(key);
        if (is != null) {
            result = resizer.load(is);
            if (result == null) {
                diskCache.delete(key); //the image must have been corrupted
            }
        }
        return result;
    }

    private void putInDiskCache(String key, final Bitmap bitmap) {
        diskCache.put(key, new DiskCache.Writer() {
            @Override
            public void write(OutputStream os) {
                bitmap.compress(bitmapCompressFormat, bitmapCompressQuality, os);
            }
        });

    }

    private void putInMemoryCache(String key, final Bitmap bitmap) {
        final boolean inCache;
        inCache = memoryCache.contains(key);
        if (!inCache) {
            memoryCache.put(key, bitmap);
        }

        bitmapReferenceCounter.acquireBitmap(bitmap);
        bitmapReferenceCounter.markPending(bitmap);
    }

    private static String getKey(String id, String transformationId, Downsampler downsampler, int width, int height) {
        return String.valueOf(Util.hash(id.hashCode(), downsampler.getId().hashCode(),
                transformationId.hashCode(), width, height));
    }
}
