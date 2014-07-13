package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

/**
 * Scales the image uniformly (maintaining the image's aspect ratio) so that one of the dimensions of the image
 * will be equal to the given dimension and the other will be less than the given dimension.
 */
public class FitCenter extends BitmapTransformation {

    public FitCenter(Context context) {
        super(context);
    }

    public FitCenter(BitmapPool bitmapPool) {
        super(bitmapPool);
    }

    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        return TransformationUtils.fitCenter(toTransform, pool, outWidth, outHeight);
    }

    @Override
    public String getId() {
        return "FitCenter.com.bumptech.glide.load.resource.bitmap";
    }
}


