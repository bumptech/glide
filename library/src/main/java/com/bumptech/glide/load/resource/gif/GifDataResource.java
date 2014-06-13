package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.Resource;

public class GifDataResource extends Resource<GifData> {
    private GifData gifData;

    public GifDataResource(GifData gifData) {
        this.gifData = gifData;
    }

    @Override
    public GifData get() {
        return gifData;
    }

    @Override
    public int getSize() {
        return gifData.getByteSize();
    }

    @Override
    protected void recycleInternal() {
        gifData.recycle();
    }
}
