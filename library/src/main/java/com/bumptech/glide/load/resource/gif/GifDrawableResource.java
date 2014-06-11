package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.Resource;

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
