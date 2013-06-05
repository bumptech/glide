/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.resize;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import com.bumptech.photos.resize.bitmap_recycle.BitmapPool;
import com.bumptech.photos.resize.bitmap_recycle.ConcurrentBitmapPool;
import com.bumptech.photos.resize.bitmap_recycle.ConcurrentBitmapReferenceCounter;
import com.bumptech.photos.resize.cache.DiskCache;
import com.bumptech.photos.resize.cache.DiskCacheAdapter;
import com.bumptech.photos.resize.cache.LruPhotoCache;
import com.bumptech.photos.resize.cache.MemoryCache;
import com.bumptech.photos.resize.cache.MemoryCacheAdapter;
import com.bumptech.photos.resize.cache.disk.AndroidDiskCache;
import com.bumptech.photos.util.Log;
import com.bumptech.photos.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

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
    private static final String DISK_CACHE_DIR = "image_manager_disk_cache";
    private static final int MAX_DISK_CACHE_SIZE = 30 * 1024 * 1024;
    private final ConcurrentBitmapReferenceCounter bitmapTracker;

    /**
     * A class for setting options for an ImageManager
     *
     * Boolean use options for the caches superseed sizes, and invalid * sizes (<= 0) are equivalent to setting the
     * corresponding use option to false.
     */
    public static class Options {
        /**
         * If true caches bitmaps in memory.
         *
         * Defaults to true
         */
        public boolean useMemoryCache = true;

        /**
         * The maximum memory cache size. This should be decreased on devices where recycling Bitmaps is possible and
         * enabled because the Bitmap cache used to recycle Bitmaps will take a substantial amount of memory.
         *
         * Defaults to 1/8th of the total application memory
         */
        public int maxMemorySize;

        /**
         * If true, caches resized bitmaps on disk.
         */
        public boolean useDiskCache = true;

        /**
         * The maximum disk cache size.
         *
         * Defaults to 30mb
         */
        public int maxDiskCacheSize;

       /**
        * The output format used to store bitmaps on disk in the disk cache
        */
        public Bitmap.CompressFormat diskCacheFormat = Bitmap.CompressFormat.JPEG;

        /**
         * If true, will attempt to recycle Bitmaps and all loaded Bitmaps will be mutable. If true and a memory cache
         * is used, the memory cache size should be decreased since the Bitmap cache used to recycle Bitmaps will
         * take a substantial amount of memory.
         *
         * Defaults to true if Android version is 3.0 or greater and will always be false, regardless of this attribute
         * otherwise.
         */
        public boolean recycleBitmaps = true;

        /**
         * The maximum number of recycled bitmaps of any requested size to keep around. Only used if recycleBitmaps
         * is true. A higher number means loads are more likely to be able to reuse a bitmap but also that this object
         * will use more memory. Increase this if there are few varieties of bitmaps that will be scrolled rapidly (ie
         * a GridView of images with lots of columns), and decrease it if there are a lot of different sizes of bitmaps
         * and limited memory is available.
         *
         * Defaults to 20
         */
        public int maxPerSize = 0;

        /**
         * Options for loading bitmaps. Some of these fields will be overwritten, including inSampleSize, inBitmap,
         * and maybe inMutable depending on how recycleBitmaps is set.
         *
         * Config and dither for example can be set
         */
        public BitmapFactory.Options bitmapDecodeOptions = ImageResizer.getDefaultOptions();

    }

    public static final boolean CAN_RECYCLE = Build.VERSION.SDK_INT >= 11;

    private final Handler mainHandler = new Handler();
    private final Handler bgHandler;
    private final ExecutorService executor;
    private final MemoryCache memoryCache;
    private final ImageResizer resizer;
    private final DiskCache diskCache;
    private final Bitmap.CompressFormat diskCacheFormat;

    private enum ResizeType {
        CENTER_CROP,
        FIT_CENTER,
        APPROXIMATE,
        AS_IS
    }

    /**
     * Try to get the external cache directory if available and default to the internal. Use a default name for the
     * cache directory if no name is provided
     */
    public static File getPhotoCacheDir(Context context) {
        return getPhotoCacheDir(context, DISK_CACHE_DIR);
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

    private static DiskCache buildDiskCacheFor(Options options, File diskCacheDir) {
        if (options.useDiskCache && options.maxDiskCacheSize <= 0) {
            options.maxDiskCacheSize = MAX_DISK_CACHE_SIZE;
        }
        final DiskCache result;
        if (diskCacheDir == null || !options.useDiskCache) {
            result = new DiskCacheAdapter();
        } else {
            result = AndroidDiskCache.get(diskCacheDir, options.maxDiskCacheSize);
        }

        return result;

    }

    private static MemoryCache buildMemoryCache(Options options, Context context) {
        if (options.useMemoryCache && options.maxMemorySize <= 0) {
            options.maxMemorySize = LruPhotoCache.getMaxCacheSize(context);
        }

        final MemoryCache result;
        if (!options.useMemoryCache) {
            result = new MemoryCacheAdapter();
        } else {
            result = new LruPhotoCache(options.maxMemorySize);
        }
        return result;
    }

    /**
     * Create an ImageManager using the default options. Note that this will create a single background thread to use
     * to resize and load images from disk. Must be created in the UI thread!
     *
     * @param context A Context used once to find or create a directory for the disk cache. This reference will not
     *                be retained by this ImageManager object and is only used in the constructor so it is safe to pass
     *                in Activities.
     */
    public ImageManager(Context context) {
        this(context, new Options());
    }


    /**
     * Create an ImageManager using the given options and that performs image loads and resize operations using the
     * given Executor. Must be created in the UI thread!
     *
     * @param context A Context used once to find or create a directory for the disk cache. This reference will not
     *                be retained by this ImageManager object and is only used in the constructor so it is safe to pass
     *                in Activities.
     * @param options The specified options
     */
    public ImageManager(Context context, Options options) {
        this(context, Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors())), options);
    }

    /**
     * Create an ImageManager using the given options and that performs image loads and resize operations using the
     * given Executor. Can be created in any thread, but the mainHandler must be a Handler for the UI thread.
     *
     * @param context A Context used once to find or create a directory for the disk cache. This reference will not
     *                be retained by this ImageManager object and is only used in the constructor so it is safe to pass
     *                in Activities.
     * @param resizeService An executor service that will be used to resize photos
     * @param options The specified option
     */
    public ImageManager(Context context, ExecutorService resizeService, Options options) {
        this(context, getPhotoCacheDir(context), resizeService, options);
    }

    /**
     * Create an ImageManager using the given options and that performs image loads and resize operations using the
     * given Executor. Can be created in any thread, but the mainHandler must be a Handler for the UI thread.
     *
     * @param context A Context used once to find or create a directory for the disk cache. This reference will not
     *                be retained by this ImageManager object and is only used in the constructor so it is safe to pass
     *                in Activities.
     * @param diskCacheDir The directory containing the disk cache or in which to create a disk cache if one does not
     *                     already exist
     * @param resizeService An executor service that will be used to resize photos
     * @param options The specified options
     */
    public ImageManager(Context context, File diskCacheDir, ExecutorService resizeService, Options options) {
        this(context, buildDiskCacheFor(options, diskCacheDir), resizeService, options);
    }

    public ImageManager(Context context, DiskCache diskCache, ExecutorService resizeService, Options options) {
        this(buildMemoryCache(options, context), diskCache, resizeService, options);
    }

    public ImageManager(MemoryCache memoryCache, DiskCache diskCache, ExecutorService resizeService, Options options) {
        HandlerThread bgThread = new HandlerThread("bg_thread");
        bgThread.start();
        bgHandler = new Handler(bgThread.getLooper());
        executor = resizeService;

        diskCacheFormat = options.diskCacheFormat;

        final BitmapPool bitmapPool;
        if (options.recycleBitmaps && CAN_RECYCLE) {
            memoryCache.setImageRemovedListener(new MemoryCache.ImageRemovedListener() {
                @Override
                public void onImageRemoved(Bitmap bitmap) {
                    releaseBitmap(bitmap);
                }
            });
            bitmapPool = new ConcurrentBitmapPool(options.maxPerSize);
            bitmapTracker = new ConcurrentBitmapReferenceCounter(bitmapPool, options.maxPerSize);
        } else {
            if (CAN_RECYCLE)
                options.bitmapDecodeOptions.inMutable = false;
            bitmapPool = null;
            bitmapTracker = null;
        }

        this.memoryCache = memoryCache;
        this.diskCache = diskCache;
        this.resizer = new ImageResizer(bitmapPool, options.bitmapDecodeOptions);
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
        return runJob(key, cb, new ImageManagerJob(key, cb, false) {
            @Override
            protected Bitmap resizeIfNotFound() throws FileNotFoundException{
                return resizer.loadAsIs(path);
            }
        });
    }

    public Object getImage(final InputStream is1, final InputStream is2, String id, LoadedCallback cb) {
        final int key = getKey(id, -1, -1, ResizeType.AS_IS);
        return runJob(key, cb, new ImageManagerJob(key, cb, false) {
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
        return runJob(key, cb, new ImageManagerJob(key, cb, false) {
            @Override
            protected Bitmap resizeIfNotFound() throws FileNotFoundException{
                return resizer.loadAsIs(path, width, height);
            }
        });
    }

    public Object getImageExact(final InputStream is, final int width, final int height, String id, LoadedCallback cb) {
        final int key = getKey(id, width, height, ResizeType.AS_IS);
        return runJob(key, cb, new ImageManagerJob(key, cb, false) {
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
        return runJob(key, cb, new ImageManagerJob(key, cb) {
            @Override
            protected Bitmap resizeIfNotFound() throws FileNotFoundException{
                return resizer.loadAtLeast(path, width, height);
            }
        });
    }

    public Object getImageApproximate(final InputStream is1, final InputStream is2, final String id, final int width, final int height, final LoadedCallback cb) {
        final int key = getKey(id, width, height, ResizeType.APPROXIMATE);
        return runJob(key, cb, new ImageManagerJob(key, cb) {
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
        return runJob(key, cb, new ImageManagerJob(key, cb) {
            @Override
            protected Bitmap resizeIfNotFound() throws FileNotFoundException{
                return resizer.centerCrop(path, width, height);
            }
        });
    }

    public Object centerCrop(final InputStream is1, final InputStream is2, final String id, final int width, final int height, final LoadedCallback cb) {
        final int key = getKey(id, width, height, ResizeType.CENTER_CROP);
        return runJob(key, cb, new ImageManagerJob(key, cb) {
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
        return runJob(key, cb, new ImageManagerJob(key, cb) {
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
        if (bitmapTracker == null) return;
        bitmapTracker.rejectBitmap(b);
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
        if (bitmapTracker == null) return;
        bitmapTracker.acquireBitmap(b);
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
        if (bitmapTracker == null) return;
        bitmapTracker.releaseBitmap(b);
    }

    /**
     * An internal method notifying the tracker that this bitmap is referenced but not necessarily used
     * by an external object. These bitmaps will not be recycled if their references drop to 0 unless they are
     * first accepted or are rejected before or after their references drop to 0. This is used because the memory cache
     * can force a bitmap to be removed b/c of size constraints while a callback referencing that bitmap is still
     * on the queue of the main thread waiting to be called. If the bitmap were not marked and the memory cache released
     * the bitmap before the callback was called on the main thread, then the bitmap would be placed in the queue to be
     * recycled once by the memory cache and then again by the object owning the callback.
     *
     * @param b The bitmap to mark
     */
    private void markBitmapPending(final Bitmap b) {
        if (bitmapTracker == null) return;

        bitmapTracker.markPending(b);
    }

    private void initBitmapTracker(final Bitmap b) {
        if (bitmapTracker == null) return;

        bitmapTracker.initBitmap(b);
    }

    public void cancelTask(Object token) {
        if (token != null) {
            ImageManagerJob job = (ImageManagerJob) token;
            job.cancel();
        }
    }

    public void shutdown() {
        executor.shutdown();
        bgHandler.getLooper().quit();
    }

    private Object runJob(int key,final LoadedCallback cb, final ImageManagerJob job) {
        final Object token = job;
        if (!returnFromCache(key, cb)) {
            job.execute();
        }
        return token;
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
        private final int key;
        private final LoadedCallback cb;
        private final boolean useDiskCache;
        private Future future = null;
        private volatile boolean cancelled = false;

        public ImageManagerJob(int key, LoadedCallback cb) {
            this(key, cb, true);
        }

        public ImageManagerJob(int key, LoadedCallback cb, boolean useDiskCache) {
            this.key = key;
            this.cb = cb;
            this.useDiskCache = useDiskCache;
        }

        public void execute() {
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

            final boolean isInDiskCache;
            String path = null;
            if (useDiskCache) {
                path = diskCache.get(String.valueOf(key));
            }

            isInDiskCache = path != null;

            Bitmap result = null;
            if (isInDiskCache) {
                try {
                    result = resizer.loadAsIs(path);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (result == null) {
                if (cancelled) return;
                try {
                    future = executor.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Bitmap result = resizeIfNotFound();
                                finishResize(result, isInDiskCache);
                            } catch (Exception e) {
                                cb.onLoadFailed(e);
                            }
                        }
                    });
                //in almost every case will be because of race after calling shutdown. Not much we can do
                //either way
                } catch (RejectedExecutionException e) {
                    e.printStackTrace();
                    cb.onLoadFailed(e);
                }
            } else {
                finishResize(result, isInDiskCache);
            }
        }

        private void finishResize(final Bitmap result, boolean isInDiskCache) {
            if (result != null) {
                if (useDiskCache && !isInDiskCache) {
                    diskCache.put(String.valueOf(key), result, diskCacheFormat);
                }

                initBitmapTracker(result);

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
            markBitmapPending(bitmap);
        }
    }

    private static int getKey(String path, int width, int height, ResizeType type){
        return Util.hash(path.hashCode(), width, height, type.hashCode());
    }
}
