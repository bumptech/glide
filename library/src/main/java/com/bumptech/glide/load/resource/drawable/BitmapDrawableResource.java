package com.bumptech.glide.load.resource.drawable;

import android.graphics.drawable.BitmapDrawable;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Util;

public class BitmapDrawableResource extends Resource<BitmapDrawable> {
    private BitmapDrawable drawable;
    private BitmapPool bitmapPool;

    public BitmapDrawableResource(BitmapDrawable drawable, BitmapPool bitmapPool) {
        this.drawable = drawable;
        this.bitmapPool = bitmapPool;
    }

    @Override
    public BitmapDrawable get() {
        return drawable;
    }

    @Override
    public int getSize() {
        return Util.getSize(drawable.getBitmap());
    }

    @Override
    protected void recycleInternal() {
        bitmapPool.put(drawable.getBitmap());
    }
}
