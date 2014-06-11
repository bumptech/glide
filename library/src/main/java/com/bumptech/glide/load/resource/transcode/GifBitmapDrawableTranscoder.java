package com.bumptech.glide.load.resource.transcode;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.resource.drawable.DrawableResource;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmap;

public class GifBitmapDrawableTranscoder implements ResourceTranscoder<GifBitmap, Drawable> {
    private final Resources resources;

    public GifBitmapDrawableTranscoder(Context context) {
        resources = context.getResources();
    }

    @Override
    public Resource<Drawable> transcode(Resource<GifBitmap> toTranscode) {
        GifBitmap gifBitmap = toTranscode.get();
        Resource<Bitmap> bitmapResource = gifBitmap.getBitmapResource();

        final Resource resource;
        final Drawable drawable;
        if (bitmapResource != null) {
            resource = bitmapResource;
            drawable = new BitmapDrawable(resources, bitmapResource.get());
        } else {
            resource = gifBitmap.getGifResource();
            drawable = gifBitmap.getGifResource().get().getDrawable();
        }
        return new DrawableResource(drawable, resource);
    }

    @Override
    public String getId() {
        return "GifBitmapDrawableTranscoder.com.bumptech.glide.load.resource.transcode";
    }
}
