/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.resize;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import com.bumptech.photos.resize.bitmap_recycle.BitmapPool;
import com.bumptech.photos.resize.bitmap_recycle.BitmapReferenceCounter;
import com.bumptech.photos.resize.bitmap_recycle.BitmapReferenceCounterAdapter;
import com.bumptech.photos.resize.bitmap_recycle.ConcurrentBitmapPool;
import com.bumptech.photos.resize.bitmap_recycle.ConcurrentBitmapReferenceCounter;
import com.bumptech.photos.resize.cache.DiskCache;
import com.bumptech.photos.resize.cache.DiskCacheAdapter;
import com.bumptech.photos.resize.cache.DiskLruCacheWrapper;
import com.bumptech.photos.resize.cache.LruPhotoCache;
import com.bumptech.photos.resize.cache.MemoryCache;
import com.bumptech.photos.resize.cache.MemoryCacheAdapter;
import com.bumptech.photos.util.Log;
import com.bumptech.photos.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
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
    private static final float MEMORY_SIZE_RATIO = 1f/10f;
    public static final boolean CAN_RECYCLE = Build.VERSION.SDK_INT >= 11;

    private final BitmapReferenceCounter bitmapReferenceCounter;
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
    /*
     *    Can only call after context is created (ie in onCreate or later...)
     */
    public static int getMaxCacheSize(Context context){
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return Math.round(MEMORY_SIZE_RATIO * activityManager.getMemoryClass() * 1024 * 1024);
    }

    /**
     * Try to get the external cache directory if available and default to the internal. Use a default name for the
     * cache directory if no name is provided
     */
    public static File getPhotoCacheDir(Context context) {
        return getPhotoCacheDir(context, DEFAULT_DISK_CACHE_DIR);
    }

    public static File getPhotoCacheDir(Context context, String cacheName) {
        String cachePath = null;

        Boolean isExternalStorageRemoveable = null;
        if (Build.VERSION.SDK_INT >= 9) {
            isExternalStorageRemoveable = Environment.isExternalStorageRemovable();
        }

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                (isExternalStorageRemoveable != null && !isExternalStorageRemoveable)) {
            //seems like this can still be null even if the above are true
            final File externalCacheDir = context.getExternalCacheDir();
            if (externalCacheDir != null) {
                cachePath = externalCacheDir.getPath();
            } else {
                Log.e("IM: external cache dir is null");
            }
        }

        if (cachePath == null) {
            final File internalCacheDir = context.getCacheDir();
            if (internalCacheDir != null) {
                cachePath = internalCacheDir.getPath();
            } else {
                Log.e("IM: internal cache dir is null");
            }
        }

        if (cachePath != null) {
            File result = new File(cachePath + File.separatorChar + cacheName);
            if (!result.exists()) {
                result.mkdir();
            }
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
        private int maxBitmapsPerSize = 20;

        public BitmapFactory.Options decodeBitmapOptions = ImageResizer.getDefaultOptions();

        public Builder(Context context) {
            this.context = context;
        }

        public ImageManager build() {
            setDefaults();

            return new ImageManager(this);
        }

        public Builder setResizeService(ExecutorService resizeService) {
            this.resizeService = resizeService;
            return this;
        }

        public Builder setBitmapCompressFormat(Bitmap.CompressFormat bitmapCompressFormat) {
            this.bitmapCompressFormat = bitmapCompressFormat;
            return this;
        }

        public Builder setRecycleBitmaps(boolean recycleBitmaps) {
            this.recycleBitmaps = recycleBitmaps && CAN_RECYCLE;
            return this;
        }

        public Builder setMemoryCache(MemoryCache memoryCache) {
            this.memoryCache = memoryCache;
            return this;
        }

        public Builder disableMemoryCache() {
            if (memoryCache != null) {
                throw new IllegalArgumentException("Can't disable memory cache after setting it");
            }
            memoryCache = new MemoryCacheAdapter();
            return this;
        }

        public Builder setDiskCache(DiskCache diskCache) {
            this.diskCache = diskCache;
            return this;
        }

        public Builder disableDiskCache() {
            diskCache = new DiskCacheAdapter();
            return this;
        }


        public Builder setMaxBitmapsPerSize(int maxBitmapsPerSize) {
            this.maxBitmapsPerSize = maxBitmapsPerSize;
            return this;
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
                memoryCache = new LruPhotoCache(getMaxCacheSize(context));
            }

            if (diskCache == null) {
                diskCache = DiskLruCacheWrapper.get(getPhotoCacheDir(context), DEFAULT_DISK_CACHE_SIZE);
            }
        }
    }

    public ImageManager(Builder builder) {
        HandlerThread bgThread = new HandlerThread("bg_thread");
        bgThread.start();
        bgHandler = new Handler(bgThread.getLooper());
        executor = builder.resizeService;
        bitmapCompressFormat = builder.bitmapCompressFormat;
        memoryCache = builder.memoryCache;
        diskCache = builder.diskCache;

        final BitmapPool bitmapPool;
        if (builder.recycleBitmaps) {
            Log.d("IM: recycle bitmaps total per size=" + builder.maxBitmapsPerSize);
            memoryCache.setImageRemovedListener(new MemoryCache.ImageRemovedListener() {
                @Override
                public void onImageRemoved(Bitmap removed) {
                    releaseBitmap(removed);
                }
            });
            bitmapPool = new ConcurrentBitmapPool(builder.maxBitmapsPerSize);
            bitmapReferenceCounter = new ConcurrentBitmapReferenceCounter(bitmapPool, builder.maxBitmapsPerSize);
        } else {
            bitmapPool = null;
            bitmapReferenceCounter = new BitmapReferenceCounterAdapter();
        }

        this.resizer = new ImageResizer(bitmapPool, builder.decodeBitmapOptions);
    }

    /**
     * Loads the image at the given path at its original dimensions.
     *
     * @param path The path id to the image
     * @param cb The callback called when the load completes
     * @return A token tracking this request
     */
    public Object getImage(final String path, final LoadedCallback cb){
        final int key = getKey(path, -1, -1, ResizeType.AS_IS);
        return runJob(key, cb, false, new ImageManagerJob() {
            @Override
            protected Bitmap resizeIfNotFound() throws FileNotFoundException{
                return resizer.loadAsIs(path);
            }
        });
    }

    public Object getImage(final InputStream is1, final InputStream is2, String id, LoadedCallback cb) {
        final int key = getKey(id, -1, -1, ResizeType.AS_IS);
        return runJob(key, cb, false, new ImageManagerJob() {
            @Override
            protected Bitmap resizeIfNotFound() throws FileNotFoundException {
                return resizer.loadAsIs(is1, is2);
            }
        });
    }

    /**
     * Loads the image for the given path assuming its width and height are exactly those given.
     *
     * @param path The path to the image
     * @param width The width of the image on disk
     * @param height The height of the image on disk
     * @param cb The callback called when the load completes
     * @return A token tracking this request
     */
    public Object getImageExact(final String path, final int width, final int height, final LoadedCallback cb) {
        final int key = getKey(path, width, height, ResizeType.AS_IS);
        return runJob(key, cb, new ImageManagerJob() {
            @Override
            protected Bitmap resizeIfNotFound() throws FileNotFoundException{
                return resizer.loadAsIs(path, width, height);
            }
        });
    }

    public Object getImageExact(final InputStream is, final int width, final int height, String id, LoadedCallback cb) {
        final int key = getKey(id, width, height, ResizeType.AS_IS);
        return runJob(key, cb, new ImageManagerJob() {
            @Override
            protected Bitmap resizeIfNotFound() throws FileNotFoundException {
                return resizer.loadAsIs(is, width, height);
            }
        });
    }

    /**
     * Loads the image for the given path to nearly the given width and height maintaining the original proportions.
     *
     * @param path The id of the image
     * @param width The desired width in pixels
     * @param height The desired height of the slice
     * @param cb The callback called when the task finishes
     * @return A token tracking this request
     */
    public Object getImageApproximate(final String path, final int width, final int height, final LoadedCallback cb){
        final int key = getKey(path, width, height, ResizeType.APPROXIMATE);
        return runJob(key, cb, new ImageManagerJob() {
            @Override
            protected Bitmap resizeIfNotFound() throws FileNotFoundException{
                return resizer.loadAtLeast(path, width, height);
            }
        });
    }

    public Object getImageApproximate(final InputStream is1, final InputStream is2, final String id, final int width, final int height, final LoadedCallback cb) {
        final int key = getKey(id, width, height, ResizeType.APPROXIMATE);
        return runJob(key, cb, new ImageManagerJob() {
            @Override
            protected Bitmap resizeIfNotFound() throws FileNotFoundException {
                return resizer.loadAtLeast(is1, is2, width, height);
            }
        });
    }

    /**
     * Loads the image for the given path , resizes it to be exactly width pixels wide keeping proportions,
     * and then returns a section from the center of image exactly height pixels tall.
     *
     * @param path The id of the image
     * @param width The desired width in pixels
     * @param height The desired height of the slice
     * @param cb The callback called when the task finishes
     * @return A token tracking this request
     */
    public Object centerCrop(final String path, final int width, final int height, final LoadedCallback cb){
        final int key = getKey(path, width, height, ResizeType.CENTER_CROP);
        return runJob(key, cb, new ImageManagerJob() {
            @Override
            protected Bitmap resizeIfNotFound() throws FileNotFoundException{
                return resizer.centerCrop(path, width, height);
            }
        });
    }

    public Object centerCrop(final InputStream is1, final InputStream is2, final String id, final int width, final int height, final LoadedCallback cb) {
        final int key = getKey(id, width, height, ResizeType.CENTER_CROP);
        return runJob(key, cb, new ImageManagerJob() {
            @Override
            protected Bitmap resizeIfNotFound() throws FileNotFoundException {
                return resizer.centerCrop(is1, is2, width, height);
            }
        });
    }

    /**
     * Loads the image for the given id and resizes it, maintaining the original proportions, so that the image fills
     * an area of width*height.
     *
     * @param path The id of the image
     * @param width The width of the space
     * @param height The height of the space
     * @param cb The callback called when the task finishes
     * @return A token tracking this request
     */
    public Object fitCenter(final String path, final int width, final int height, final LoadedCallback cb){
        final int key = getKey(path, width, height, ResizeType.FIT_CENTER);
        return runJob(key, cb, new ImageManagerJob() {
            @Override
            protected Bitmap resizeIfNotFound() throws FileNotFoundException{
                return resizer.fitInSpace(path, width, height);
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
    public void acquireBitmap(Bitmap b) {
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

    public void cancelTask(Object token) {
        if (token != null) {
            ImageManagerJob job = (ImageManagerJob) token;
            job.cancel();
        }
    }

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
        private int key;
        private LoadedCallback cb;
        private boolean useDiskCache;
        private Future future = null;
        private volatile boolean cancelled = false;

        public void execute(int key, LoadedCallback cb, boolean useDiskCache) {
            this.key = key;
            this.cb = cb;
            this.useDiskCache = useDiskCache;
            bgHandler.post(this);
        }

        public void cancel() {
            if (cancelled) return;
            cancelled = true;

            if (bgHandler != null) {
                bgHandler.removeCallbacks(this);
            }
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
                result = diskCache.get(stringKey, new DiskCache.Reader() {
                    @Override
                    public Bitmap read(InputStream is1, InputStream is2) {
                        Bitmap result = resizer.loadAsIs(is1, is2);
                        if (result == null) {
                            diskCache.delete(stringKey);
                        }
                        return result;
                    }
                });
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

                    try {
                        finishResize(resizeIfNotFound(), false);
                    } catch (Exception e) {
                        cb.onLoadFailed(e);
                    }
                }
            });
        }

        private void finishResize(final Bitmap result, boolean isInDiskCache) {
            if (result != null) {
                if (useDiskCache && !isInDiskCache) {
                    diskCache.put(String.valueOf(key), new DiskCache.Writer() {
                        @Override
                        public void write(OutputStream os) {
                            result.compress(bitmapCompressFormat, 100, os);
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
            }
        }

        protected abstract Bitmap resizeIfNotFound() throws FileNotFoundException;
    }


    private void putInMemoryCache(int key, Bitmap bitmap) {
        if (memoryCache.put(key, bitmap) != bitmap) {
            acquireBitmap(bitmap);
            bitmapReferenceCounter.markPending(bitmap);
        }
    }

    private static int getKey(String path, int width, int height, ResizeType type){
        return Util.hash(path.hashCode(), width, height, type.hashCode());
    }
}
