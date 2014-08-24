package com.bumptech.svgsample.app;

import com.bumptech.glide.load.engine.Resource;

public class SvgResource extends Resource<Svg> {
    private Svg svg;

    public SvgResource(Svg svg) {
        this.svg = svg;
    }

    @Override
    public Svg get() {
        return svg;
    }

    @Override
    public int getSize() {
        return 0; // return the byte size of the svg.
    }

    @Override
    protected void recycleInternal() {
        // Return any resources you can to some pool to be reused by your decoder.
    }
}
