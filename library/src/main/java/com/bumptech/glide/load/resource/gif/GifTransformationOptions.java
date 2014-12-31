package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.TransformationOptions;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.FitCenter;

public final class GifTransformationOptions extends TransformationOptions<GifTransformationOptions, GifDrawable> {
    private final Context context;

    public static GifTransformationOptions withFitCenter(Context context) {
        return new GifTransformationOptions(context).fitCenter();
    }

    public static GifTransformationOptions withCenterCrop(Context context) {
        return new GifTransformationOptions(context).centerCrop();
    }

    public static GifTransformationOptions withBitmapTransform(Context context, Transformation<Bitmap> transformation) {
        return new GifTransformationOptions(context).transformBitmap(transformation);
    }

    public GifTransformationOptions(Context context) {
        this.context = context.getApplicationContext();
    }

    public GifTransformationOptions centerCrop() {
        return transformBitmap(new CenterCrop(context));
    }

    public GifTransformationOptions fitCenter() {
        return transformBitmap(new FitCenter(context));
    }

    public GifTransformationOptions transformBitmap(Transformation<Bitmap> transformation) {
        return transform(new GifDrawableTransformation(transformation, Glide.get(context).getBitmapPool()));
    }

    @Override
    protected void applyCenterCrop() {
        centerCrop();
    }

    @Override
    protected void applyFitCenter() {
        super.applyFitCenter();
    }
}
