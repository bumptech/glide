package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.Resource;

//TODO: make this safe for multiple consumers.
public class GifResource extends Resource<GifData> {
    private GifData gifData;

    public GifResource(GifData gifData) {
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
