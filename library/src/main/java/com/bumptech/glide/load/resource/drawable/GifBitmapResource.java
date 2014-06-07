package com.bumptech.glide.load.resource.drawable;

import com.bumptech.glide.Resource;

public class GifBitmapResource extends Resource<GifBitmap> {
    private GifBitmap data;

    public GifBitmapResource(GifBitmap data) {
        this.data = data;
    }

    @Override
    public GifBitmap get() {
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
