package com.bumptech.glide.load.resource.bitmap;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Util;

/**
 * A resource wrapper for {@link com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable}.
 */
public class GlideBitmapDrawableResource extends Resource<GlideBitmapDrawable> {
    private GlideBitmapDrawable drawable;
    private BitmapPool bitmapPool;
    private boolean returnedOriginalDrawable;

    public GlideBitmapDrawableResource(GlideBitmapDrawable drawable, BitmapPool bitmapPool) {
        this.drawable = drawable;
        this.bitmapPool = bitmapPool;
    }

    @Override
    public GlideBitmapDrawable get() {
        final GlideBitmapDrawable result;
        if (!returnedOriginalDrawable) {
            result = drawable;
            returnedOriginalDrawable = true;
        } else {
            result = (GlideBitmapDrawable) drawable.getConstantState().newDrawable();
        }
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
