/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos;

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
public class PhotoManager {
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

    public PhotoManager(int maxMemCacheSize, long maxDiskCacheSize, File diskCacheDir, Handler mainHandler, Handler backgroundHandler) {
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
        final Object token = cb;
        final String key = getKey(path, 0, 0, ResizeType.AS_IS);
        if (!returnFromCache(key, cb)) {
            final Runnable task = resizer.loadAsIs(path, getResizeCb(key, token, cb, false, false));
            postJob(task, token);
        }
        return token;
    }

    /**
     * Loads the image for the given id to nearly the given width and height maintaining the original proportions
     * @param path - the id of the image
     * @param width - the desired width in pixels
     * @param height - the desired height of the slice
     * @param cb - the callback called when the task finishes
     * @return A token tracking this request
     */
    public Object getImage(final String path, final int width, final int height, final LoadedCallback cb){
        final Object token = cb;
        final String key = getKey(path, width, height, ResizeType.APPROXIMATE);
        if (!returnFromCache(key, cb)) {
            Runnable checkDiskCache = diskCache.get(key, new DiskCacheCallback(key, token, cb) {
                @Override
                public Runnable resizeIfNotFound(LoadedCallback resizeCallback) {
                    return resizer.loadApproximate(path, width, height, resizeCallback);
                }
            });
            postJob(checkDiskCache, token);
        }
        return token;
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
        final Object token = cb;
        final String key = getKey(path, width, height, ResizeType.CENTER_CROP);
        if (!returnFromCache(key, cb)) {
            Runnable checkDiskCache = diskCache.get(key, new DiskCacheCallback(key, token, cb) {
                @Override
                public Runnable resizeIfNotFound(LoadedCallback resizeCallback) {
                    return resizer.resizeCenterCrop(path, width, height, resizeCallback);
                }
            });
            postJob(checkDiskCache, token);
        }
        return token;
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
        final Object token = cb;
        final String key = getKey(path, width, height, ResizeType.FIT_CENTER);
        if (!returnFromCache(key, cb)) {
            Runnable checkDiskCache = diskCache.get(key, new DiskCacheCallback(key, token, cb) {
                @Override
                public Runnable resizeIfNotFound(LoadedCallback resizeCallback) {
                    return resizer.fitInSpace(path, width, height, resizeCallback);
                }
            });
            postJob(checkDiskCache, token);
        }
        return token;
    }

    private boolean returnFromCache(String key, LoadedCallback cb) {
        boolean found = false;
        Bitmap inCache = memoryCache.get(key);
        if (inCache != null) {
            found = true;
            cb.onLoadCompleted(inCache);
        }
        return found;
    }

    private abstract class DiskCacheCallback implements PhotoDiskCache.GetCallback {
        private Object token;
        private LoadedCallback cb;
        private final String key;

        public DiskCacheCallback(String key, Object token, LoadedCallback cb) {
            this.key = key;
            this.token = token;
            this.cb = cb;
        }

        @Override
        public void onGet(InputStream is1, InputStream is2) {
            final Runnable task;
            final boolean inDiskCache = is1 != null && is2 != null;
            final LoadedCallback resizeCb = getResizeCb(key, token, cb, inDiskCache, true);
            if (inDiskCache) {
                task = resizer.loadAsIs(is1, is2, resizeCb);
            } else {
                task = resizeIfNotFound(resizeCb);
            }
            postJob(task, token);
        }

        public abstract Runnable resizeIfNotFound(LoadedCallback cb);
    }

    private LoadedCallback getResizeCb(final String key, final Object token, final LoadedCallback cb, final boolean inDiskCache, final boolean useDiskCache) {
        return new LoadedCallback() {
            @Override
            public void onLoadCompleted(Bitmap resized) {
                memoryCache.put(key, resized);
                acquireBitmap(resized);
                if (!inDiskCache && useDiskCache) {
                    Runnable putToDiskCache = diskCache.put(key, resized);
                    postJob(putToDiskCache, null);
                }
                cb.onLoadCompleted(resized);
            }

            @Override
            public void onLoadFailed(Exception e) {
                cb.onLoadFailed(e);
            }
        };
    }

    private void postJob(Runnable job, Object token) {
        backgroundHandler.postAtTime(job, token, SystemClock.uptimeMillis());
    }

    public void cancelTask(Object token){
        if (token != null) {
            backgroundHandler.removeCallbacksAndMessages(token);
        }
    }

    public void rejectBitmap(Bitmap b) {
        if (!CAN_RECYCLE) return;

        Integer currentCount = bitmapReferenceCounter.get(b.hashCode());
        if (currentCount == null || currentCount == 0) {
            bitmapReferenceCounter.remove(b.hashCode());
            bitmapCache.put(b);
        }
    }

    public void acquireBitmap(Bitmap b) {
        if (!CAN_RECYCLE) return;

        Integer currentCount = bitmapReferenceCounter.get(b.hashCode());
        if (currentCount == null) {
            currentCount = 0;
        }
        bitmapReferenceCounter.put(b.hashCode(), currentCount + 1);
    }

    public void releaseBitmap(Bitmap b) {
        if (!CAN_RECYCLE) return;

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
