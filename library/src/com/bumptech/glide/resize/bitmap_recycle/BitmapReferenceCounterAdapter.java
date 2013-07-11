package com.bumptech.glide.resize.bitmap_recycle;

import android.graphics.Bitmap;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 6/5/13
 * Time: 3:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class BitmapReferenceCounterAdapter implements BitmapReferenceCounter {
    @Override
    public void initBitmap(Bitmap bitmap) { }

    @Override
    public void acquireBitmap(Bitmap bitmap) { }

    @Override
    public void releaseBitmap(Bitmap bitmap) { }

    @Override
    public void rejectBitmap(Bitmap bitmap) { }

    @Override
    public void markPending(Bitmap bitmap) { }
}
