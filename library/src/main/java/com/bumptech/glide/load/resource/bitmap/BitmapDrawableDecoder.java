package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.io.IOException;

public class BitmapDrawableDecoder<T> implements ResourceDecoder<T, BitmapDrawable> {

    private Resources resources;
    private BitmapPool bitmapPool;
    private ResourceDecoder<T, Bitmap> wrapped;

    public BitmapDrawableDecoder(Context context, ResourceDecoder<T, Bitmap> decoder) {
        this(context.getResources(), Glide.get(context).getBitmapPool(), decoder);
    }

    public BitmapDrawableDecoder(Resources resources, BitmapPool bitmapPool, ResourceDecoder<T, Bitmap> wrapped) {
        this.resources = resources;
        this.bitmapPool = bitmapPool;
        this.wrapped = wrapped;
    }

    @Override
    public boolean handles(T source) throws IOException {
        return wrapped.handles(source);
    }

    @Override
    public Resource<BitmapDrawable> decode(T source, int width, int height) throws IOException {
        Resource<Bitmap> bitmapResource = wrapped.decode(source, width, height);
        BitmapDrawable drawable = new BitmapDrawable(resources, bitmapResource.get());

        return new BitmapDrawableResource(drawable, bitmapPool);
    }
}
