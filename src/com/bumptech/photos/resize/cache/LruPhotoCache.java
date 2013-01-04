/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.resize.cache;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/**
 * A thin wrapper around the LruCache provided in the Android support libraries.
 *
 * @see android.support.v4.util.LruCache
 */
public class LruPhotoCache{
    private static final float SIZE_RATIO = 1f/8f;
    private final PhotoCache photoCache;
    private PhotoRemovedListener photoRemovedListener;

    public interface PhotoRemovedListener {
        public void onPhotoRemoved(String key, Bitmap bitmap);
    }

    /*
    Can only call after context is created (ie in onCreate or later...)
    */
    public static int getMaxCacheSize(Context context){
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return Math.round(SIZE_RATIO * activityManager.getMemoryClass() * 1024 * 1024);
    }

    private class PhotoCache extends LruCache<String, Bitmap> {

        private PhotoCache(int maxSize) {
            super(maxSize);
        }


        @Override
        protected int sizeOf(String key, Bitmap value) {
            //get the size, getByteCount() is API 12+...
            return value.getHeight() * value.getRowBytes();
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
            super.entryRemoved(evicted, key, oldValue, newValue);    //To change body of overridden methods use File | Settings | File Templates.
            if (photoRemovedListener != null) {
                photoRemovedListener.onPhotoRemoved(key, oldValue);
            }
        }
    }

    public LruPhotoCache(int size) {
        photoCache = new PhotoCache(size);
    }

    public void setPhotoRemovedListener(PhotoRemovedListener listener) {
        this.photoRemovedListener = listener;
    }

    public void put(String key, Bitmap bitmap) {
        photoCache.put(key, bitmap);
    }

    public Bitmap get(String key) {
        return photoCache.get(key);
    }

    public void remove(String key){
        photoCache.remove(key);
    }

    public void evictAll(){
        photoCache.evictAll();
    }
}
