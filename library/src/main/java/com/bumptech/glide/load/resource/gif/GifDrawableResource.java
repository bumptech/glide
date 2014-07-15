package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.load.engine.Resource;

/**
 * A resource wrapping an {@link com.bumptech.glide.load.resource.gif.GifData} resource to return
 * {@link com.bumptech.glide.load.resource.gif.GifDrawable}s.
 */
public class GifDrawableResource extends Resource<GifDrawable> {
    private Resource<GifData> wrapped;

    public GifDrawableResource(Resource<GifData> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public GifDrawable get() {
        return wrapped.get().getDrawable();
    }

    @Override
    public int getSize() {
        return wrapped.getSize();
    }

    @Override
    protected void recycleInternal() {
        wrapped.recycle();
    }
}
