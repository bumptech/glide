package com.bumptech.glide.load.resource.transcode;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.resource.drawable.BitmapDrawableResource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

public class BitmapDrawableTranscoder implements ResourceTranscoder<Bitmap, BitmapDrawable> {
    private Resources resources;
    private BitmapPool bitmapPool;

    public BitmapDrawableTranscoder(Resources resources, BitmapPool bitmapPool) {
        this.resources = resources;
        this.bitmapPool = bitmapPool;
    }

    @Override
    public Resource<BitmapDrawable> transcode(Resource<Bitmap> toTranscode) {
        BitmapDrawable drawable = new BitmapDrawable(resources, toTranscode.get());
        return new BitmapDrawableResource(drawable, bitmapPool);
    }

    @Override
    public String getId() {
        return "BitmapDrawableTranscoder.com.bumptech.glide.load.data.transcode";
    }
}
