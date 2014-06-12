package com.bumptech.glide.load.resource.gifbitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.resource.gif.GifData;

public class GifBitmapWrapper {
    private final Resource<GifData> gifResource;
    private final Resource<Bitmap> bitmapResource;

    public GifBitmapWrapper(Resource<Bitmap> bitmapResource, Resource<GifData> gifResource) {
        this.bitmapResource = bitmapResource;
        this.gifResource = gifResource;
    }

    public int getSize() {
        return 0;
    }

    public Resource<Bitmap> getBitmapResource() {
        return bitmapResource;
    }

    public Resource<GifData> getGifResource() {
        return gifResource;
    }
}
