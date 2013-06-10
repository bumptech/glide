package com.bumptech.photos.resize.cache;

import android.graphics.Bitmap;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 6/5/13
 * Time: 10:21 AM
 * To change this template use File | Settings | File Templates.
 */
public interface DiskCache {
    public String get(String key);
    public void put(String key, Bitmap value, Bitmap.CompressFormat format);
    public void delete(String key);
}
