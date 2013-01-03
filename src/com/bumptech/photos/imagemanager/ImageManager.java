/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.imagemanager;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import com.bumptech.photos.imagemanager.cache.LruPhotoCache;
import com.bumptech.photos.imagemanager.cache.PhotoDiskCache;
import com.bumptech.photos.imagemanager.cache.SizedBitmapCache;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Created by IntelliJ IDEA.
 * User: sam
 * Date: 2/9/12
 * Time: 5:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImageManager {
    private static final String DISK_CACHE_DIR = "image_manager_disk_cache";

    public static class Options {
        public int appVersion = 0;

        public boolean useMemoryCache = true;
        public int maxMemorySize;

        public boolean useDiskCache = true;
        public int maxDiskCacheSize = 10 * 1024 * 1024;

        public boolean recycleBitmaps = true;
    }

    public static final boolean CAN_RECYCLE = Build.VERSION.SDK_INT >= 11;

    private final Handler mainHandler;
    private final LruPhotoCache memoryCache;
    private final ImageResizer resizer;
    private final Executor executor;
    private final Map<Integer, Integer> bitmapReferenceCounter = new HashMap<Integer, Integer>();
    private final SizedBitmapCache bitmapCache;
    private final PhotoDiskCache diskCache;
    private final boolean isBitmapRecyclingEnabled;

    private enum ResizeType {
        CENTER_CROP,
        FIT_CENTER,
        APPROXIMATE,
        AS_IS
    }

    public static File getPhotoCacheDir(Context context) {
        final String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }

        File result = new File(cachePath + File.separatorChar + DISK_CACHE_DIR);
        if (!result.exists()) {
            result.mkdir();
        }
        return result;
    }

    public ImageManager(Context context) {
        this(context, new Options());
    }

    public ImageManager(Context context, Options options) {
        this(context, new HandlerExecutor(), options);
    }

    public ImageManager(Context context, Executor executor, Options options) {
        this(context, executor, new Handler(), options);
    }

    public ImageManager(Context context, Executor executor, Handler mainHandler, Options options) {
        this(context, getPhotoCacheDir(context), executor, mainHandler, options);
    }

    public ImageManager(Context context, File diskCacheDir, Executor executor, Handler mainHandler, Options options) {
        isBitmapRecyclingEnabled = options.recycleBitmaps && CAN_RECYCLE;
        if (options.useMemoryCache && options.maxMemorySize <= 0) {
            options.maxMemorySize = LruPhotoCache.getMaxCacheSize(context);
        }

        if (diskCacheDir == null || !options.useDiskCache || options.maxDiskCacheSize <= 0) {
            diskCache = null;
        } else {
            diskCache = new PhotoDiskCache(diskCacheDir, options.maxDiskCacheSize, options.appVersion);
        }

        if (!options.useMemoryCache || options.maxMemorySize <= 0) {
            memoryCache = null;
        } else {
            memoryCache = new LruPhotoCache(options.maxMemorySize);
        }

        if (isBitmapRecyclingEnabled) {
            if (memoryCache != null) {
                memoryCache.setPhotoRemovedListener(new LruPhotoCache.PhotoRemovedListener() {
                    @Override
                    public void onPhotoRemoved(String key, Bitmap bitmap) {
                        releaseBitmap(bitmap);
                    }
                });
            }
            bitmapCache = new SizedBitmapCache();
        } else {
            bitmapCache = null;
        }

        this.resizer = new ImageResizer(bitmapCache);
        this.mainHandler = mainHandler;
        this.executor = executor;
    }

    /**
     * Loads the image for the given id
     * @param path - the path id to the image
     * @param cb - the callback called when the load completes
     * @return A token tracking this request
     */
    public Object getImage(final String path, final LoadedCallback cb){
        final String key = getKey(path, 0, 0, ResizeType.AS_IS);
        return runJob(key, cb, new ImageManagerJob(key, cb, false) {
            @Override
            protected Bitmap resizeIfNotFound() {
                return resizer.loadAsIs(path);
            }
        });
    }

    /**
     * Loads the image for the given id assuming its width and height are exactly those given
     * @param path - the path to the image
     * @param width - the width of the image on disk
     * @param height - the height of the image on disk
     * @param cb - the callback called when the load completes
     * @return A token tracking this request
     */
    public Object getImageExact(final String path, final int width, final int height, final LoadedCallback cb) {
        final String key = getKey(path, width, height, ResizeType.AS_IS);
        return runJob(key, cb, new ImageManagerJob(key, cb, false) {
            @Override
            protected Bitmap resizeIfNotFound() {
                return resizer.loadAsIs(path, width, height);
            }
        });
    }

    /**
     * Loads the image for the given id to nearly the given width and height maintaining the original proportions
     * @param path - the id of the image
     * @param width - the desired width in pixels
     * @param height - the desired height of the slice
     * @param cb - the callback called when the task finishes
     * @return A token tracking this request
     */
    public Object getImageApproximate(final String path, final int width, final int height, final LoadedCallback cb){
        final String key = getKey(path, width, height, ResizeType.APPROXIMATE);
        return runJob(key, cb, new ImageManagerJob(key, cb) {
            @Override
            protected Bitmap resizeIfNotFound() {
                return resizer.loadApproximate(path, width, height);
            }
        });
    }

    /**
     * Loads the image for the given id, resizes it to be exactly width pixels wide keeping proportions,
     * and then returns a section from the center of image exactly height pixels tall
     * @param path - the id of the image
     * @param width - the desired width in pixels
     * @param height - the desired height of the slice
     * @param cb - the callback called when the task finishes
     * @return A token tracking this request
     */
    public Object centerCrop(final String path, final int width, final int height, final LoadedCallback cb){
        final String key = getKey(path, width, height, ResizeType.CENTER_CROP);
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
     * @param path - the id of the image
     * @param width - the width of the space
     * @param height - the height of the space
     * @param cb - the callback called when the task finishes
     * @return A token tracking this request
     */
    public Object fitCenter(final String path, final int width, final int height, final LoadedCallback cb){
        final String key = getKey(path, width, height, ResizeType.FIT_CENTER);
        return runJob(key, cb, new ImageManagerJob(key, cb) {
            @Override
            protected Bitmap resizeIfNotFound() {
                return resizer.fitInSpace(path, width, height);
            }
        });
    }

    private Object runJob(String key,final LoadedCallback cb, ImageManagerJob job) {
        final Object token = cb;
        if (!returnFromCache(key, cb)) {
            executor.execute(job);
        }
        return token;
    }

    private boolean returnFromCache(String key, LoadedCallback cb) {
        boolean found = false;
        Bitmap inCache = getFromMemoryCache(key);
        if (inCache != null) {
            found = true;
            cb.onLoadCompleted(inCache);
        }
        return found;
    }

    private abstract class ImageManagerJob implements Runnable {
        private final String key;
        private final LoadedCallback cb;
        private final boolean useDiskCache;

        public ImageManagerJob(String key, LoadedCallback cb) {
            this(key, cb, true);
        }

        public ImageManagerJob(String key, LoadedCallback cb, boolean useDiskCache) {
            this.key = key;
            this.cb = cb;
            this.useDiskCache = useDiskCache;
        }

        @Override
        public void run() {
            InputStream is1 = getFromDiskCache(key);
            InputStream is2 = null;
            if (is1 != null) {
                is2 = getFromDiskCache(key);
            }

            final boolean isInDiskCache = is1 != null && is2 != null;
            Bitmap result = null;
            try {
                if (isInDiskCache && useDiskCache) {
                    result = resizer.loadAsIs(is1, is2);
                } else {
                    result = resizeIfNotFound();
                }
            } catch (Exception e) {
                cb.onLoadFailed(e);
            }

            if (result != null) {
                final Bitmap finalResult = result;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        acquireBitmap(finalResult);
                        putInMemoryCache(key, finalResult);
                        cb.onLoadCompleted(finalResult);
                    }
                });
                //this is time consuming so do it after posting the result
                if (!isInDiskCache && useDiskCache) {
                    putInDiskCache(key, result);
                }
            }
        }

        protected abstract Bitmap resizeIfNotFound();
    }

    private InputStream getFromDiskCache(String key) {
        InputStream result = null;
        if (diskCache != null) {
            result = diskCache.get(key);
        }
        return result;
    }

    private void putInDiskCache(String key, Bitmap value) {
        if (diskCache != null) {
            diskCache.put(key, value);
        }
    }

    private Bitmap getFromMemoryCache(String key) {
        Bitmap result = null;
        if (memoryCache != null) {
            result = memoryCache.get(key);
        }
        return result;
    }

    private void putInMemoryCache(String key, Bitmap bitmap) {
        if (memoryCache != null) {
            memoryCache.put(key, bitmap);
        }
    }

    public void rejectBitmap(Bitmap b) {
        if (!isBitmapRecyclingEnabled) return;

        Integer currentCount = bitmapReferenceCounter.get(b.hashCode());
        if (currentCount == null || currentCount == 0) {
            bitmapReferenceCounter.remove(b.hashCode());
            bitmapCache.put(b);
        }
    }

    public void acquireBitmap(Bitmap b) {
        if (!isBitmapRecyclingEnabled) return;

        Integer currentCount = bitmapReferenceCounter.get(b.hashCode());
        if (currentCount == null) {
            currentCount = 0;
        }
        bitmapReferenceCounter.put(b.hashCode(), currentCount + 1);
    }

    public void releaseBitmap(Bitmap b) {
        if (!isBitmapRecyclingEnabled) return;

        Integer currentCount = bitmapReferenceCounter.get(b.hashCode()) - 1;
        if (currentCount == 0) {
            bitmapReferenceCounter.remove(b.hashCode());
            bitmapCache.put(b);
        } else {
            bitmapReferenceCounter.put(b.hashCode(), currentCount);
        }
    }

    private static String getKey(String path, int width, int height, ResizeType type){
        return path + width + "_" + height + type.name();
    }
}
