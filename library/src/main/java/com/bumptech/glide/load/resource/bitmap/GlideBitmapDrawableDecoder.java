package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.io.IOException;

public class GlideBitmapDrawableDecoder<T> implements ResourceDecoder<T, GlideBitmapDrawable> {

    private Resources resources;
    private BitmapPool bitmapPool;
    private ResourceDecoder<T, Bitmap> wrapped;

    public GlideBitmapDrawableDecoder(Context context, ResourceDecoder<T, Bitmap> decoder) {
        this(context.getResources(), Glide.get(context).getBitmapPool(), decoder);
    }

    public GlideBitmapDrawableDecoder(Resources resources, BitmapPool bitmapPool, ResourceDecoder<T, Bitmap> wrapped) {
        this.resources = resources;
        this.bitmapPool = bitmapPool;
        this.wrapped = wrapped;
    }

    @Override
    public boolean handles(T source) {
        return wrapped.handles(source);
    }

    @Override
    public Resource<GlideBitmapDrawable> decode(T source, int width, int height) throws IOException {
        Resource<Bitmap> bitmapResource = wrapped.decode(source, width, height);
        GlideBitmapDrawable drawable = new GlideBitmapDrawable(resources, bitmapResource.get());

        return new GlideBitmapDrawableResource(drawable, bitmapPool);
    }
}
