package com.bumptech.svgsample.app;

import android.graphics.drawable.PictureDrawable;
import com.bumptech.glide.load.engine.Resource;

/**
 * Resource wrapping a {@link PictureDrawable}.
 */
public class SvgDrawableResource extends Resource<PictureDrawable> {
    private final PictureDrawable svgDrawable;

    public SvgDrawableResource(PictureDrawable svgDrawable) {
        this.svgDrawable = svgDrawable;
    }

    @Override
    public PictureDrawable get() {
        return svgDrawable;
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    protected void recycleInternal() {
        // can't recycle PictureDrawables
    }
}
