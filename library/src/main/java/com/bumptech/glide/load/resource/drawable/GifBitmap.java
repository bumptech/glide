package com.bumptech.glide.load.resource.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.resource.gif.GifDrawable;

public class GifBitmap {
    private Resource<GifDrawable> gifResource;
    private Resource<Bitmap> bitmapResource;
    private Resources resources;

    public GifBitmap(Resources resources, Resource<Bitmap> bitmapResource) {
        this.resources = resources;
        this.bitmapResource = bitmapResource;
    }

    public GifBitmap(Resource<GifDrawable> gifResource) {
        this.gifResource = gifResource;
    }

    public int getSize() {
        return 0;
    }

    public Resource<Bitmap> getBitmapResource() {
        return bitmapResource;
    }

    public Resource<GifDrawable> getGifResource() {
        return gifResource;
    }

    public Drawable getDrawable() {
        if (gifResource != null) {
            return gifResource.get();
        } else {
            return new BitmapDrawable(resources, bitmapResource.get());
        }
    }
}
