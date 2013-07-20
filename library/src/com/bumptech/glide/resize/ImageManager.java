/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.resize;

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
import com.bumptech.glide.resize.bitmap_recycle.ConcurrentBitmapReferenceCounter;
import com.bumptech.glide.resize.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.resize.cache.DiskCache;
import com.bumptech.glide.resize.cache.DiskCacheAdapter;
import com.bumptech.glide.resize.cache.DiskLruCacheWrapper;
import com.bumptech.glide.resize.cache.LruPhotoCache;
import com.bumptech.glide.resize.cache.MemoryCache;
import com.bumptech.glide.resize.cache.MemoryCacheAdapter;
import com.bumptech.glide.util.Log;
import com.bumptech.glide.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
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
 * sdk version, uses a  combination of an LRU disk cache and an LRU hard memory cache to try to reduce the number of
 * load and resize * operations performed and to maximize the number of times Bitmaps are recycled as opposed to
 * allocated.
 *
 * If no options are given defaults to using both a memory and a disk cache and to recycling bitmaps if possible. Note
 * that Bitmap recycling is only available on Honeycomb and up.
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

    private enum ResizeType {
        CENTER_CROP,
        FIT_CENTER,
        APPROXIMATE,
        AS_IS
    }

    /**
     * Get the maximum safe memory cache size for this particular device based on the # of mb allocated to each app.
     * This is a conservative estimate that has been safe for 2.2+ devices consistnetly. It is probably rather small
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
    public static File getPhotoCacheDir(Context context, String cacheName) {
        File cacheDir = null;

        Boolean isExternalStorageRemoveable = null;
        if (Build.VERSION.SDK_INT >= 9) {
            isExternalStorageRemoveable = Environment.isExternalStorageRemovable();
        }

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                (isExternalStorageRemoveable != null && !isExternalStorageRemoveable)) {
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
        } else {
            Log.d("IM: default disk cache dir is null");
            return null;
        }
    }

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
         * @see {@link Bitmap#compress(android.graphics.Bitmap.CompressFormat, int, java.io.OutputStream)}
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
         * @see {@link Bitmap#compress(android.graphics.Bitmap.CompressFormat, int, java.io.OutputStream)}
         * @see {@link #setBitmapCompressFormat(android.graphics.Bitmap.CompressFormat)}
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
                memoryCache = new LruPhotoCache(getSafeMemoryCacheSize(context));
            }

            if (diskCache == null) {
                try {
                    diskCache = DiskLruCacheWrapper.get(getPhotoCacheDir(context), DEFAULT_DISK_CACHE_SIZE);
                } catch (IOException e) {
                    //this is probably a corrupt or full sd card, so default to not using a disk cache
                    e.printStackTrace();
                    diskCache = new DiskCacheAdapter();
                }
            }

            if (!recycleBitmaps) {
                bitmapPool = new BitmapPoolAdapter();
                bitmapReferenceCounter = new BitmapReferenceCounterAdapter();
            } else {
                if (bitmapPool == null) {
                    bitmapPool = new LruBitmapPool(getSafeMemoryCacheSize(context));
                }
                bitmapReferenceCounter = new ConcurrentBitmapReferenceCounter(bitmapPool);
            }
        }
    }

    private ImageManager(Builder builder) {
        HandlerThread bgThread = new HandlerThread("bg_thread");
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

    /**
     * Loads the image at its original dimensions.
     *
     * @param id A string id that uniquely identifies the image to be loaded. It may include the width and height, but
     *           is not required to do so
     * @param streamLoader The {@link StreamLoader} that will be used to load the image if it is not cached
     * @param cb The callback called when the load completes
     * @return A token tracking this request
     */
    public Object getImage(String id, final StreamLoader streamLoader, final LoadedCallback cb){
        final int key = getKey(id, -1, -1, ResizeType.AS_IS);
        return runJob(key, cb, false, new ImageManagerJob(streamLoader) {
            @Override
            protected Bitmap resizeIfNotFound(InputStream is) throws IOException {
                return resizer.loadAsIs(is);
            }
        });
    }

    /**
     * Loads the image assuming its width and height are exactly those given.
     *
     * @param id A string id that uniquely identifies the image to be loaded. It may include the width and height, but
     *           is not required to do so
     * @param streamLoader The {@link StreamLoader} that will be used to load the image if it is not cached
     * @param width The width of the image on disk
     * @param height The height of the image on disk
     * @param cb The callback called when the load completes
     * @return A token tracking this request
     */
    public Object getImageExact(String id, StreamLoader streamLoader, final int width, final int height, final LoadedCallback cb) {
        final int key = getKey(id, width, height, ResizeType.AS_IS);
        return runJob(key, cb, new ImageManagerJob(streamLoader) {
            @Override
            protected Bitmap resizeIfNotFound(InputStream is) throws FileNotFoundException{
                return resizer.loadAsIs(is, width, height);
            }
        });
    }

    /**
     * Loads the image to nearly the given width and height maintaining the original proportions.
     *
     * @param id A string id that uniquely identifies the image to be loaded. It may include the width and height, but
     *           is not required to do so
     * @param streamLoader The {@link StreamLoader} that will be used to load the image if it is not cached
     * @param width The desired width in pixels
     * @param height The desired height of the slice
     * @param cb The callback called when the task finishes
     * @return A token tracking this request
     */
    public Object getImageApproximate(String id, StreamLoader streamLoader, final int width, final int height, final LoadedCallback cb) {
        final int key = getKey(id, width, height, ResizeType.APPROXIMATE);
        return runJob(key, cb, new ImageManagerJob(streamLoader) {
            @Override
            protected Bitmap resizeIfNotFound(InputStream is) throws FileNotFoundException {
                return resizer.loadAtLeast(is, width, height);
            }
        });
    }

    /**
     * Loads the image, resizes it to be exactly width pixels wide keeping proportions,
     * and then returns a section from the center of image exactly height pixels tall.
     *
     * @param id A string id that uniquely identifies the image to be loaded. It may include the width and height, but
     *           is not required to do so
     * @param streamLoader The {@link StreamLoader} that will be used to load the image if it is not cached
     * @param width The desired width in pixels
     * @param height The desired height of the slice
     * @param cb The callback called when the task finishes
     * @return A token tracking this request
     */
    public Object centerCrop(String id, StreamLoader streamLoader, final int width, final int height, final LoadedCallback cb) {
        final int key = getKey(id, width, height, ResizeType.CENTER_CROP);
        return runJob(key, cb, new ImageManagerJob(streamLoader) {
            @Override
            protected Bitmap resizeIfNotFound(InputStream is) throws FileNotFoundException {
                return resizer.centerCrop(is, width, height);
            }
        });
    }

    /**
     * Loads the image for the given id and resizes it, maintaining the original proportions, so that the image fills
     * an area of width*height.
     *
     * @param id A string id that uniquely identifies the image to be loaded. It may include the width and height, but
     *           is not required to do so
     * @param streamLoader The {@link StreamLoader} that will be used to load the image if it is not cached
     * @param width The width of the space
     * @param height The height of the space
     * @param cb The callback called when the task finishes
     * @return A token tracking this request
     */
    public Object fitCenter(String id, StreamLoader streamLoader, final int width, final int height, final LoadedCallback cb){
        final int key = getKey(id, width, height, ResizeType.FIT_CENTER);
        return runJob(key, cb, new ImageManagerJob(streamLoader) {
            @Override
            protected Bitmap resizeIfNotFound(InputStream is) throws FileNotFoundException{
                return resizer.fitInSpace(is, width, height);
            }
        });
    }

    /**
     * Notify the ImageManager that a bitmap it loaded is not going to be displayed and can go into a queue to be
     * reused. Does nothing if recycling is disabled or impossible.
     *
     * @param b The rejected Bitmap
     */
    public void rejectBitmap(final Bitmap b) {
        bgHandler.post(new Runnable() {
            @Override
            public void run() {
                bitmapReferenceCounter.rejectBitmap(b);
            }
        });
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
        bgHandler.post(new Runnable() {
            @Override
            public void run() {
                bitmapReferenceCounter.acquireBitmap(b);
            }
        });
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
        bgHandler.post(new Runnable() {
            @Override
            public void run() {
                bitmapReferenceCounter.releaseBitmap(b);
            }
        });
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
    public void shutdown() {
        shutdown = true;
        executor.shutdown();
        bgHandler.getLooper().quit();
    }

    private Object runJob(int key, LoadedCallback cb, ImageManagerJob job) {
        return runJob(key, cb, true, job);
    }

    private Object runJob(int key, LoadedCallback cb, boolean useDiskCache, ImageManagerJob job) {
        if (shutdown) return null;

        if (!returnFromCache(key, cb)) {
            job.execute(key, cb, useDiskCache);
        }
        return job;
    }

    private boolean returnFromCache(int key, LoadedCallback cb) {
        Bitmap inCache = memoryCache.get(key);
        boolean found = inCache != null;
        if (found) {
            cb.onLoadCompleted(inCache);
        }
        return found;
    }

    private abstract class ImageManagerJob implements Runnable {
        private final StreamLoader streamLoader;
        private int key;
        private LoadedCallback cb;
        private boolean useDiskCache;
        private Future future = null;
        private volatile boolean cancelled = false;

        public ImageManagerJob(StreamLoader streamLoader) {
            this.streamLoader = streamLoader;
        }

        public void execute(int key, LoadedCallback cb, boolean useDiskCache) {
            this.key = key;
            this.cb = cb;
            this.useDiskCache = useDiskCache;
            bgHandler.post(this);
        }

        public void cancel() {
            if (cancelled) return;
            cancelled = true;

            bgHandler.removeCallbacks(this);
            if (future != null) {
                future.cancel(false);
            }
        }

        @Override
        public void run() {
            if (cancelled) return;

            final String stringKey = String.valueOf(key);
            Bitmap result = null;
            if (useDiskCache) {
                final InputStream is = diskCache.get(stringKey);
                if (is != null) {
                    result = resizer.loadAsIs(is);
                    if (result == null) {
                        diskCache.delete(stringKey); //the image must have been corrupted
                    }
                }
            }

            if (result == null) {
                try {
                    resizeWithPool();
                } catch (Exception e) {
                    cb.onLoadFailed(e);
                }
            } else {
                finishResize(result, true);
            }
        }

        //in almost every case exception will be because of race after calling shutdown. Not much we can do
        //either way
        private void resizeWithPool() throws RejectedExecutionException {
            future = executor.submit(new Runnable() {
                @Override
                public void run() {
                    if (cancelled) return;

                    streamLoader.loadStream(new StreamLoader.StreamReadyCallback() {
                        @Override
                        public void onStreamReady(final InputStream is) {
                            future = executor.submit(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        final Bitmap result = resizeIfNotFound(is);
                                        finishResize(result, false);
                                    } catch (Exception e) {
                                        cb.onLoadFailed(e);
                                    }
                                }
                            });
                        }

                        @Override
                        public void onException(Exception e) {
                            cb.onLoadFailed(e);
                        }
                    });
                }
            });
        }

        private void finishResize(final Bitmap result, boolean isInDiskCache) {
            if (result != null) {
                if (useDiskCache && !isInDiskCache) {
                    diskCache.put(String.valueOf(key), new DiskCache.Writer() {
                        @Override
                        public void write(OutputStream os) {
                            result.compress(bitmapCompressFormat, bitmapCompressQuality, os);
                        }
                    });
                }

                bitmapReferenceCounter.initBitmap(result);
                putInMemoryCache(key, result);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        cb.onLoadCompleted(result);
                    }
                });
            } else {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        cb.onLoadFailed(null);
                    }
                });
            }
        }

        protected abstract Bitmap resizeIfNotFound(InputStream is) throws IOException;
    }


    private void putInMemoryCache(int key, Bitmap bitmap) {
        final boolean inCache;
        synchronized (memoryCache) {
            inCache = memoryCache.contains(key);
            if (!inCache) {
                memoryCache.put(key, bitmap);
            }
        }

        if (!inCache) {
            acquireBitmap(bitmap);
            bitmapReferenceCounter.markPending(bitmap);
        }
    }

    private static int getKey(String id, int width, int height, ResizeType type){
        return Util.hash(id.hashCode(), width, height, type.hashCode());
    }
}
