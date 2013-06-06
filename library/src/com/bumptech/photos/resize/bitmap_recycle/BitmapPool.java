package com.bumptech.photos.resize.bitmap_recycle;

import android.graphics.Bitmap;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 6/5/13
 * Time: 2:52 PM
 * To change this template use File | Settings | File Templates.
 */
public interface BitmapPool {
    public boolean put(Bitmap bitmap);
    public Bitmap get(int width, int height);

}
