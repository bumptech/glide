package com.bumptech.glide.load.resource.drawable;

import android.graphics.drawable.Drawable;
import com.bumptech.glide.Resource;

public class DrawableResource extends Resource<Drawable> {
    private final Drawable drawable;
    private final Resource wrapped;

    public DrawableResource(Drawable drawable, Resource wrapped) {
        this.drawable = drawable;
        this.wrapped = wrapped;
    }

    @Override
    public Drawable get() {
        return drawable;
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
