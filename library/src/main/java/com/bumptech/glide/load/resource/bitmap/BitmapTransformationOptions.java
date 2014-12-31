package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.TransformationOptions;

public final class BitmapTransformationOptions extends TransformationOptions<BitmapTransformationOptions, Bitmap> {

    private final Context context;

    public static BitmapTransformationOptions withFitCenter(Context context) {
        return new BitmapTransformationOptions(context).fitCenter();
    }

    public static BitmapTransformationOptions withCenterCrop(Context context) {
        return new BitmapTransformationOptions(context).centerCrop();
    }

    public BitmapTransformationOptions(Context context) {
        this.context = context.getApplicationContext();
    }

    public BitmapTransformationOptions centerCrop() {
        return transform(new CenterCrop(context));
    }

    public BitmapTransformationOptions fitCenter() {
        return transform(new FitCenter(context));
    }

    @Override
    protected void applyFitCenter() {
        fitCenter();
    }

    @Override
    protected void applyCenterCrop() {
        centerCrop();
    }
}
