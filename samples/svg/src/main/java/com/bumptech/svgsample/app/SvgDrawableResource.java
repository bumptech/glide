package com.bumptech.svgsample.app;

import com.bumptech.glide.load.engine.Resource;

public class SvgDrawableResource extends Resource<SvgDrawable> {
    private SvgDrawable svgDrawable;

    public SvgDrawableResource(SvgDrawable svgDrawable) {
        this.svgDrawable = svgDrawable;
    }

    @Override
    public SvgDrawable get() {
        return svgDrawable;
    }

    @Override
    public int getSize() {
        // Return the byte size of the SVG.
        return 1234;
    }

    @Override
    protected void recycleInternal() {
        // Return any resources you can to some pool to be reused by your decoder.
    }
}
