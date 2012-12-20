/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import com.bumptech.photos.cache.LruPhotoCache;
import com.bumptech.photos.cache.PhotoDiskCache;
import com.bumptech.photos.resize.PhotoStreamResizer;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by IntelliJ IDEA.
 * User: sam
 * Date: 2/9/12
 * Time: 5:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class PhotoManager {
    private PhotoDiskCache diskCache;
    private LruPhotoCache memoryCache;
    private PhotoStreamResizer resizer;
    private Map<Object, Future> taskManager = new HashMap<Object, Future>();
    private Handler backgroundHandler;

    public PhotoManager(int maxMemCacheSize, long maxDiskCacheSize, File diskCacheDir, Handler mainHandler, Handler backgroundHandler) {
        this.backgroundHandler = backgroundHandler;
        if (Build.VERSION.SDK_INT < 11)
            this.memoryCache = new LruPhotoCache(maxMemCacheSize);
        this.diskCache = new PhotoDiskCache(diskCacheDir, maxDiskCacheSize, mainHandler, backgroundHandler);
        this.resizer = new PhotoStreamResizer(mainHandler, backgroundHandler);
    }

    public Object getImage(final String path, final LoadedCallback cb) {
        return getImage(path, null, cb);
    }

    /**
     * Loads the image for the given id
     * @param path - the path id to the image
     * @param recycled - a mutable bitmap of the same width and height as image at path to be reused
     * @param cb - the callback called when the load completes
     * @return A token tracking this request
     */
    public Object getImage(final String path, final Bitmap recycled, final LoadedCallback cb){
        final Object token = cb;
        final String key = getKey(path);
        if (!returnFromCache(key, token, cb)) {
            final Future task = resizer.loadAsIs(path, recycled, getResizeCb(key, token, cb, false, false));
            taskManager.put(token, task);
        }
        return token;
    }

    public Object getImage(String path, int width, int height, LoadedCallback cb) {
        return getImage(path, width, height, null, cb);
    }

    /**
     * Loads the image for the given id to nearly the given width and height maintaining the original proportions
     * @param path - the id of the image
     * @param width - the desired width in pixels
     * @param height - the desired height of the slice
     * @param recycled - a mutable bitmap of the same width and height to be reused
     * @param cb - the callback called when the task finishes
     * @return A token tracking this request
     */
    public Object getImage(final String path, final int width, final int height, final Bitmap recycled, final LoadedCallback cb){
        final Object token = cb;
        final String key = getKey(path, width, height);
        if (!returnFromCache(key, token, cb)) {
            diskCache.get(key, new DiskCacheCallback(key, token, recycled, cb) {
                @Override
                public Future resizeIfNotFound(PhotoStreamResizer.ResizeCallback resizeCallback) {
                    return resizer.loadApproximate(path, width, height, resizeCallback);
                }
            });
        }
        return token;
    }


    public Object centerSlice(String path, int width, int height, LoadedCallback cb) {
        return centerSlice(path, width, height, null, cb);
    }

    /**
     * Loads the image for the given id, resizes it to be exactly width pixels wide keeping proportions,
     * and then returns a section from the center of image exactly height pixels tall
     * @param path - the id of the image
     * @param width - the desired width in pixels
     * @param height - the desired height of the slice
     * @param recycled - a mutable bitmap of the same width and height to be reused
     * @param cb - the callback called when the task finishes
     * @return A token tracking this request
     */
    public Object centerSlice(final String path, final int width, final int height, final Bitmap recycled, final LoadedCallback cb){
        final Object token = cb;
        final String key = getKey(path, width, height);
        if (!returnFromCache(key, token, cb)) {
            diskCache.get(key, new DiskCacheCallback(key, token, recycled, cb, false) {
                @Override
                public Future resizeIfNotFound(PhotoStreamResizer.ResizeCallback resizeCallback) {
                    return resizer.resizeCenterCrop(path, width, height, resizeCallback);
                }
            });
        }
        return token;
    }

    public Object fitCenter(String path, int width, int height, LoadedCallback cb) {
        return fitCenter(path, width, height, null, cb);
    }

    /**
     * Loads the image for the given id and resizes it, maintaining the original proportions, so that the image fills
     * an area of width*height.
     * @param path - the id of the image
     * @param width - the width of the space
     * @param height - the height of the space
     * @param recycled - a mutable bitmap of the same width and height to be reused
     * @param cb - the callback called when the task finishes
     * @return A token tracking this request
     */
    public Object fitCenter(final String path, final int width, final int height, final Bitmap recycled, final LoadedCallback cb){
        final Object token = cb;
        final String key = getKey(path, width, height);
        if (!returnFromCache(key, token, cb)) {
            diskCache.get(key, new DiskCacheCallback(key, token, recycled, cb) {
                @Override
                public Future resizeIfNotFound(PhotoStreamResizer.ResizeCallback resizeCallback) {
                    return resizer.fitInSpace(path, width, height, resizeCallback);
                }
            });
        }
        return token;
    }

    private boolean returnFromCache(String key, Object token, LoadedCallback cb) {
        boolean found = false;
        if (Build.VERSION.SDK_INT < 11) {
            Bitmap inCache = memoryCache.get(key);
            if (inCache != null) {
                found = true;
                cb.loadCompleted(inCache);
            }
        }
        return found;
    }

    private abstract class DiskCacheCallback implements PhotoDiskCache.GetCallback {
        private String key;
        private Object token;
        private Bitmap recycled;
        private LoadedCallback cb;
        private boolean useDiskCache;

        public DiskCacheCallback(String key, Object token, Bitmap recycled, LoadedCallback cb, boolean useDiskCache) {
            this.key = key;
            this.token = token;
            this.recycled = recycled;
            this.cb = cb;
            this.useDiskCache = useDiskCache;
        }

        public DiskCacheCallback(String key, Object token, Bitmap recycled, LoadedCallback cb) {
            this(key, token, recycled, cb, true);
        }

        @Override
        public void onGet(InputStream is) {
            final Future task;
            final boolean inDiskCache = is != null;
            final PhotoStreamResizer.ResizeCallback resizeCb = getResizeCb(key, token, cb, inDiskCache, useDiskCache);
            if (inDiskCache) {
                task = resizer.loadAsIs(is, recycled, resizeCb);
            } else {
                task = resizeIfNotFound(resizeCb);
            }
            taskManager.put(token, task);
        }

        public abstract Future resizeIfNotFound(PhotoStreamResizer.ResizeCallback cb);
    }

    private PhotoStreamResizer.ResizeCallback getResizeCb(final String key, final Object token, final LoadedCallback cb, final boolean inDiskCache, final boolean useDiskCache ) {
        return new PhotoStreamResizer.ResizeCallback() {
            @Override
            public void onResizeComplete(Bitmap resized) {
                if (Build.VERSION.SDK_INT < 11) {
                    memoryCache.put(key, resized);
                }
                if (!inDiskCache && useDiskCache) {
                    diskCache.put(key, resized);
                }
                taskManager.remove(token);
                cb.loadCompleted(resized);
            }

            @Override
            public void onResizeFailed(Exception e) {
                cb.onLoadFailed(e);
            }
        };
    }

    public void cancelTask(Object token){
        backgroundHandler.removeCallbacksAndMessages(token);
        final Future task = taskManager.get(token);
        if (task != null){
            task.cancel(true);
        }
    }

    private static String getKey(String path){
        return getKey(path, 0, 0);
    }

    private static String getKey(String path, int width, int height){
        return sha1Hash(path) + "_" + String.valueOf(width) + "_" + String.valueOf(height);
    }

    private static String sha1Hash(String toHash) {
        String hash = null;
        try {
            byte[] bytes = toHash.getBytes();
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(bytes, 0, bytes.length);
            hash = new BigInteger(1, digest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return hash;
    }

}
