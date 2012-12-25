/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.cache;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/**
 * Created by IntelliJ IDEA.
 * User: sam
 * Date: 2/9/12
 * Time: 5:57 PM
 * To change this template use File | Settings | File Templates.
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
            Log.d("RECYCLE entryRemoved evicted=" + evicted + " oldValue=" + oldValue + " newValue=" + newValue);
            if (evicted && photoRemovedListener != null) {
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
