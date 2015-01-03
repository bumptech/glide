package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.drawable.DrawableTransformation;
import com.bumptech.glide.util.Preconditions;

public class BitmapDrawableTransformer implements DrawableTransformation.Transformer<BitmapDrawable> {
    private final Resources resources;
    private final BitmapPool bitmapPool;

    public BitmapDrawableTransformer(Context context) {
        this(context.getResources(), Glide.get(context).getBitmapPool());
   }

    BitmapDrawableTransformer(Resources resources, BitmapPool bitmapPool) {
        this.resources = Preconditions.checkNotNull(resources, "Resources must not be null");
        this.bitmapPool = Preconditions.checkNotNull(bitmapPool, "Bitmap pool must not be null");
    }

    @Override
    public Resource<BitmapDrawable> transform(Resource<BitmapDrawable> toTransform,
            Transformation<Bitmap> transformation, int width, int height) {
        BitmapDrawable drawable = toTransform.get();
        Bitmap bitmap = drawable.getBitmap();
        Resource<Bitmap> transformed = transformation.transform(BitmapResource.obtain(bitmap, bitmapPool), width,
                height);
        return new BitmapDrawableResource(new BitmapDrawable(resources, transformed.get()), bitmapPool);
    }

    @Override
    public Class<BitmapDrawable> getTransformableDrawable() {
        return BitmapDrawable.class;
    }
}
