package com.bumptech.glide.load.resource.gifbitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.gif.GifData;

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
        Resource<Bitmap> bitmapResource = data.getBitmapResource();
        if (bitmapResource != null) {
            bitmapResource.recycle();
        }
        Resource<GifData> gifDataResource = data.getGifResource();
        if (gifDataResource != null) {
            gifDataResource.recycle();
        }
    }
}
