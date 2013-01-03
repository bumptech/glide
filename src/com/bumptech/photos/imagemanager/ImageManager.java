/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.imagemanager;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import com.bumptech.photos.cache.LruPhotoCache;
import com.bumptech.photos.cache.PhotoDiskCache;
import com.bumptech.photos.cache.SizedBitmapCache;
import com.bumptech.photos.resize.ResizeJobGenerator;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: sam
 * Date: 2/9/12
 * Time: 5:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImageManager {
    public static final boolean CAN_RECYCLE = Build.VERSION.SDK_INT >= 11;

    private PhotoDiskCache diskCache;
    private LruPhotoCache memoryCache;
    private ResizeJobGenerator resizer;
    private Handler backgroundHandler;
    private Map<Integer, Integer> bitmapReferenceCounter = new HashMap<Integer, Integer>();
    private SizedBitmapCache bitmapCache = new SizedBitmapCache();

    private enum ResizeType {
        CENTER_CROP,
        FIT_CENTER,
        APPROXIMATE,
        AS_IS
    }

    public ImageManager(int maxMemCacheSize, long maxDiskCacheSize, File diskCacheDir, Handler mainHandler, Handler backgroundHandler) {
        this.backgroundHandler = backgroundHandler;
        this.memoryCache = new LruPhotoCache(maxMemCacheSize);
        memoryCache.setPhotoRemovedListener(new LruPhotoCache.PhotoRemovedListener() {
            @Override
            public void onPhotoRemoved(String key, Bitmap bitmap) {
                releaseBitmap(bitmap);
            }
        });
        this.diskCache = new PhotoDiskCache(diskCacheDir, maxDiskCacheSize, mainHandler, backgroundHandler);
        this.resizer = new ResizeJobGenerator(mainHandler, CAN_RECYCLE ? bitmapCache : null);
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
