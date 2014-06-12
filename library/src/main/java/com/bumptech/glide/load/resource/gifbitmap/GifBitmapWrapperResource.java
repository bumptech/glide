package com.bumptech.glide.load.resource.gifbitmap;

import com.bumptech.glide.Resource;

public class GifBitmapWrapperResource extends Resource<GifBitmapWrapper> {
    private GifBitmapWrapper data;

    public GifBitmapWrapperResource(GifBitmapWrapper data) {
        this.data = data;
    }

    @Override
    public GifBitmapWrapper get() {
        return data;
    }

    @Override
    public int getSize() {
        return data.getSize();
    }

    @Override
    protected void recycleInternal() {
    }
}
