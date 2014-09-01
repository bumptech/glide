package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.load.engine.Resource;

/**
 * A resource wrapping an {@link com.bumptech.glide.load.resource.gif.GifDrawable}.
 */
public class GifDrawableResource extends Resource<GifDrawable> {
    private final GifDrawable drawable;
    private boolean returnedInitial;

    public GifDrawableResource(GifDrawable drawable) {
        this.drawable = drawable;
    }

    @Override
    public GifDrawable get() {
        if (!returnedInitial) {
            returnedInitial = true;
            return drawable;
        } else {
            return (GifDrawable) drawable.getConstantState().newDrawable();
        }
    }

    @Override
    public int getSize() {
        return drawable.getData().length;
    }

    @Override
    protected void recycleInternal() {
        drawable.stop();
        drawable.recycle();
    }
}
