package com.bumptech.glide.load.resource.gifbitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.gif.GifData;

public class GifBitmapWrapper {
    private final Resource<GifData> gifResource;
    private final Resource<Bitmap> bitmapResource;

    public GifBitmapWrapper(Resource<Bitmap> bitmapResource, Resource<GifData> gifResource) {
        if (bitmapResource != null && gifResource != null) {
            throw new IllegalArgumentException("Can only contain either a bitmap resource or a gif resource, not both");
        }
        if (bitmapResource == null && gifResource == null) {
            throw new IllegalArgumentException("Must contain either a bitmap resource or a gif resource");
        }
        this.bitmapResource = bitmapResource;
        this.gifResource = gifResource;
    }

    public int getSize() {
        if (bitmapResource != null) {
            return bitmapResource.getSize();
        } else {
            return gifResource.getSize();
        }
    }

    public Resource<Bitmap> getBitmapResource() {
        return bitmapResource;
    }

    public Resource<GifData> getGifResource() {
        return gifResource;
    }
}
