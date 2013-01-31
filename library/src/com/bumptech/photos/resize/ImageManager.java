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
import android.os.SystemClock;
import com.bumptech.photos.resize.cache.LruPhotoCache;
import com.bumptech.photos.resize.cache.SizedBitmapCache;
import com.bumptech.photos.resize.cache.disk.DiskCache;
import com.bumptech.photos.util.Log;
import com.bumptech.photos.util.Util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * A class to coordinate image loading, resizing, recycling, and caching. Depending on the provided options and the
 * sdk version, uses a  combination of an LRU disk cache and an LRU hard memory cache to try to reduce the number of
 * load and resize  * operations performed and to maximize the number of times Bitmaps are recycled as opposed to
 * allocated.
 *
 * If no options are given defaults to using both a memory and a disk cache and to recycling bitmaps if possible. Note
 * that Bitmap recycling is only available on Honeycomb and up.
 */
public class ImageManager {
    private static final String DISK_CACHE_DIR = "image_manager_disk_cache";
    private static final int MAX_DISK_CACHE_SIZE = 30 * 1024 * 1024;

    /**
     * A class for setting options for an ImageManager
     *
     * Boolean use options for the caches superseed sizes, and invalid * sizes (<= 0) are equivalent to setting the
     * corresponding use option to false.
     */
    public static class Options {
        /**
         * @see com.jakewharton.DiskLruCache#open(java.io.File, int, int, long)
         */
        public int appVersion = 0;

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

    private final Handler mainHandler;
    private final Handler bgHandler;
    private final LruPhotoCache memoryCache;
    private final ImageResizer resizer;
    private final Map<Integer, Integer> bitmapReferenceCounter = new HashMap<Integer, Integer>();
    private final SizedBitmapCache bitmapCache;
    private final DiskCache diskCache;
    private final boolean isBitmapRecyclingEnabled;

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
        this(context, new Handler(), options);
    }

    /**
     * Create an ImageManager using the given options and that performs image loads and resize operations using the
     * given Executor. Can be created in any thread, but the mainHandler must be a Handler for the UI thread.
     *
     * @param context A Context used once to find or create a directory for the disk cache. This reference will not
     *                be retained by this ImageManager object and is only used in the constructor so it is safe to pass
     *                in Activities.
     * @param mainHandler A Handler to the UI thread.
     * @param options The specified options
     */
    public ImageManager(Context context, Handler mainHandler, Options options) {
        this(context, getPhotoCacheDir(context), mainHandler, options);
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
     * @param mainHandler A Handler to the UI thread.
     * @param options The specified options
     */
    public ImageManager(Context context, File diskCacheDir, Handler mainHandler, Options options) {
        HandlerThread bgThread = new HandlerThread("image_manager_bg");
        bgThread.start();
        bgHandler = new Handler(bgThread.getLooper());

        isBitmapRecyclingEnabled = options.recycleBitmaps && CAN_RECYCLE;

        if (options.useMemoryCache && options.maxMemorySize <= 0) {
            options.maxMemorySize = LruPhotoCache.getMaxCacheSize(context);
        }

        if (options.useDiskCache && options.maxDiskCacheSize <= 0) {
            options.maxDiskCacheSize = MAX_DISK_CACHE_SIZE;
        }

        if (diskCacheDir == null || !options.useDiskCache) {
            diskCache = null;
        } else {
            diskCache = DiskCache.get(diskCacheDir, options.maxDiskCacheSize);
        }

        if (!options.useMemoryCache) {
            memoryCache = null;
        } else {
            memoryCache = new LruPhotoCache(options.maxMemorySize);
        }

        if (isBitmapRecyclingEnabled) {
            if (memoryCache != null) {
                memoryCache.setPhotoRemovedListener(new LruPhotoCache.PhotoRemovedListener() {
                    @Override
                    public void onPhotoRemoved(Integer key, Bitmap bitmap) {
                        releaseBitmap(bitmap);
                    }
                });
            }
            bitmapCache = new SizedBitmapCache(options.maxPerSize);
        } else {
            if (CAN_RECYCLE)
                options.bitmapDecodeOptions.inMutable = false;
            bitmapCache = null;
        }


        this.resizer = new ImageResizer(bitmapCache, options.bitmapDecodeOptions);
        this.mainHandler = mainHandler;
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
            protected Bitmap resizeIfNotFound() {
                return resizer.loadAsIs(path);
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
            protected Bitmap resizeIfNotFound() {
                return resizer.loadAsIs(path, width, height);
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
            protected Bitmap resizeIfNotFound() {
                return resizer.loadApproximate(path, width, height);
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
            protected Bitmap resizeIfNotFound() {
                return resizer.resizeCenterCrop(path, width, height);
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
            protected Bitmap resizeIfNotFound() {
                return resizer.fitInSpace(path, width, height);
            }
        });
    }

    public void pause() {
        if (diskCache != null) {
            bgHandler.post(new Runnable() {
                @Override
                public void run() {
                    diskCache.close();
                }
            });
        }
    }

    public void resume() {
        if (diskCache != null) {
            bgHandler.post(new Runnable() {
                @Override
                public void run() {
                    diskCache.open();
                }
            });
        }
    }

    /**
     * Notify the ImageManager that a bitmap it loaded is not going to be displayed and can go into a queue to be
     * reused. Does nothing if recycling is disabled or impossible.
     *
     * @param b The rejected Bitmap
     */
    public void rejectBitmap(final Bitmap b) {
        if (!isBitmapRecyclingEnabled) return;

        Integer currentCount = bitmapReferenceCounter.get(b.hashCode());
        if (currentCount == null || currentCount == 0) {
            bitmapReferenceCounter.remove(b.hashCode());
            //can only put or take from bitmap cache on one thread
            bgHandler.post(new Runnable() {
                @Override
                public void run() {
                    bitmapCache.put(b);
                }
            });
        }
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
        if (!isBitmapRecyclingEnabled) return;

        Integer currentCount = bitmapReferenceCounter.get(b.hashCode());
        if (currentCount == null) {
            currentCount = 0;
        }
        bitmapReferenceCounter.put(b.hashCode(), currentCount + 1);
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
        if (!isBitmapRecyclingEnabled) return;

        Integer currentCount = bitmapReferenceCounter.get(b.hashCode()) - 1;
        if (currentCount == 0) {
            bitmapReferenceCounter.remove(b.hashCode());
            //can only put or take from bitmap cache on one thread
            bgHandler.postAtFrontOfQueue(new Runnable() {
                @Override
                public void run() {
                    bitmapCache.put(b);
                }
            });
        } else {
            bitmapReferenceCounter.put(b.hashCode(), currentCount);
        }
    }

    public void cancelTask(Object token) {
        if (token != null)
            bgHandler.removeCallbacksAndMessages(token);
    }


    private Object runJob(int key,final LoadedCallback cb, final ImageManagerJob job) {
        final Object token = cb;
        if (!returnFromCache(key, cb)) {
            bgHandler.postAtTime(job, token, SystemClock.uptimeMillis());
        }
        return token;
    }

    private boolean returnFromCache(int key, LoadedCallback cb) {
        Bitmap inCache = getFromMemoryCache(key);
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

        public ImageManagerJob(int key, LoadedCallback cb) {
            this(key, cb, true);
        }

        public ImageManagerJob(int key, LoadedCallback cb, boolean useDiskCache) {
            this.key = key;
            this.cb = cb;
            this.useDiskCache = useDiskCache;
        }

        @Override
        public void run() {
            final boolean isInDiskCache;
            String path = null;
            if (useDiskCache) {
                path = getFromDiskCache(key);
            }

            isInDiskCache = path != null;

            Bitmap result = null;
            if (isInDiskCache) {
                try {
                    result = resizer.loadAsIs(path);//resizer.loadAsIs(is1, is2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (result == null) {
                try {
                    result = resizeIfNotFound();
                } catch (Exception e) {
                    e.printStackTrace();
                    cb.onLoadFailed(e);
                }
            }

            if (result != null) {
                if (useDiskCache && !isInDiskCache) {
                    putInDiskCache(key, result);
                }

                final Bitmap finalResult = result;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        putInMemoryCache(key, finalResult);
                        cb.onLoadCompleted(finalResult);
                    }

                });
            }
        }

        protected abstract Bitmap resizeIfNotFound();
    }


    private String getFromDiskCache(int key) {
        String result = null;
        if (diskCache != null) {
            result = diskCache.get(String.valueOf(key));
        }
        return result;
    }

    private void putInDiskCache(int key, Bitmap value) {
        if (diskCache != null) {
            diskCache.put(String.valueOf(key), value);
        }
    }

    private Bitmap getFromMemoryCache(int key) {
        Bitmap result = null;
        if (memoryCache != null) {
            result = memoryCache.get(key);
        }
        return result;
    }

    private void putInMemoryCache(int key, Bitmap bitmap) {
        if (memoryCache != null) {
            acquireBitmap(bitmap);
            memoryCache.put(key, bitmap);
        }
    }

    private static int getKey(String path, int width, int height, ResizeType type){
        return Util.hash(path.hashCode(), width, height, type.hashCode());
    }
}
