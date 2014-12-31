package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.drawable.DrawableTransformation;

public class GifDrawableTransformer implements DrawableTransformation.Transformer<GifDrawable> {
    private final BitmapPool bitmapPool;

    public GifDrawableTransformer(Context context) {
        this(Glide.get(context).getBitmapPool());
    }

    public GifDrawableTransformer(BitmapPool bitmapPool) {
        if (bitmapPool == null) {
            throw new NullPointerException("Bitmap pool must not be null");
        }
        this.bitmapPool = bitmapPool;
    }

    @Override
    public Resource<GifDrawable> transform(Resource<GifDrawable> toTransform, Transformation<Bitmap> transformation,
            int width, int height) {
        GifDrawableTransformation gifDrawableTransformation = new GifDrawableTransformation(transformation, bitmapPool);
        return gifDrawableTransformation.transform(toTransform, width, height);
    }

    @Override
    public Class<GifDrawable> getTransformableDrawable() {
        return GifDrawable.class;
    }
}
