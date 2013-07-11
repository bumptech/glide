package com.bumptech.glide.resize.bitmap_recycle;

import android.graphics.Bitmap;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 6/5/13
 * Time: 2:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class BitmapPoolAdapter implements BitmapPool {
    @Override
    public boolean put(Bitmap bitmap) {
        return false;
    }

    @Override
    public Bitmap get(int width, int height) {
        return null;
    }
}
