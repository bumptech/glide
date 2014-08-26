package com.bumptech.glide.load.resource.bitmap;

import android.graphics.drawable.BitmapDrawable;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Util;

/**
 * A {@link com.bumptech.glide.load.engine.Resource} that wraps an {@link android.graphics.drawable.BitmapDrawable}
 * <p>
 *     This class ensures that every call to {@link #get()}} always returns a new
 *     {@link android.graphics.drawable.BitmapDrawable} to avoid rendering issues if used in multiple views and
 *     is also responsible for returning the underlying {@link android.graphics.Bitmap} to the given
 *     {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} when the resource is recycled.
 * </p>
 */
public class BitmapDrawableResource implements Resource<BitmapDrawable> {
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
    public void recycle() {
        bitmapPool.put(drawable.getBitmap());
    }
}
