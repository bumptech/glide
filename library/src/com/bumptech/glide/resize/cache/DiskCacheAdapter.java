package com.bumptech.glide.resize.cache;

import android.graphics.Bitmap;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 6/5/13
 * Time: 10:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class DiskCacheAdapter implements DiskCache {
    @Override
    public Bitmap get(String key, Reader reader) {
        return null;
    }

    @Override
    public void put(String key, Writer writer) { }

    @Override
    public void delete(String key) { }
}
