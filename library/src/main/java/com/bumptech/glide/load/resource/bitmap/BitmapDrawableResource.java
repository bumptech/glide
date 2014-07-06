package com.bumptech.glide.load.resource.bitmap;

import android.graphics.drawable.BitmapDrawable;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Util;

public class BitmapDrawableResource extends Resource<BitmapDrawable> {
    private final BitmapDrawable drawable;
    private final BitmapPool bitmapPool;

    private BitmapDrawable next;

    public BitmapDrawableResource(BitmapDrawable drawable, BitmapPool bitmapPool) {
        this.drawable = drawable;
        this.bitmapPool = bitmapPool;

        this.next = drawable;
    }

    @Override
    public BitmapDrawable get() {
        // We usually just have one consumer, so return the given drawable (created on a bg thread) to the first
        // consumer and then create a new drawable for each subsequent consumer.
        if (next == null) {
            next = (BitmapDrawable) drawable.getConstantState().newDrawable();
        }
        BitmapDrawable result = next;
        next = null;
        return result;
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
