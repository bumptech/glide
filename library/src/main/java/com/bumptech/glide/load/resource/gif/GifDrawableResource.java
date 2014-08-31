package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.load.resource.drawable.DrawableResource;

/**
 * A resource wrapping an {@link com.bumptech.glide.load.resource.gif.GifDrawable}.
 */
public class GifDrawableResource extends DrawableResource<GifDrawable> {
    public GifDrawableResource(GifDrawable drawable) {
        super(drawable);
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
