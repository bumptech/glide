package com.bumptech.photos.resize.cache;

import android.graphics.Bitmap;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 6/5/13
 * Time: 11:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class MemoryCacheAdapter implements MemoryCache {
    @Override
    public boolean contains(Integer key) {
        return false;
    }

    @Override
    public Bitmap get(Integer key) {
        return null;
    }

    @Override
    public Bitmap put(Integer key, Bitmap bitmap) {
        return null;
    }

    @Override
    public void setImageRemovedListener(ImageRemovedListener listener) { }
}
