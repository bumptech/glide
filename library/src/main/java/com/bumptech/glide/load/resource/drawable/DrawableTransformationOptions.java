package com.bumptech.glide.load.resource.drawable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.TransformationOptions;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableTransformer;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.gif.GifDrawableTransformer;

public final class DrawableTransformationOptions extends TransformationOptions<DrawableTransformationOptions, Drawable> {

    private final Context context;

    private BitmapDrawableTransformer bitmapDrawableTransformer;
    private GifDrawableTransformer gifDrawableTransformer;

    public static DrawableTransformationOptions withFitCenter(Context context) {
        return new DrawableTransformationOptions(context).fitCenter();
    }

    public static DrawableTransformationOptions withCenterCrop(Context context) {
        return new DrawableTransformationOptions(context).centerCrop();
    }

    public static DrawableTransformationOptions withBitmapTransform(Context context,
            Transformation<Bitmap> transformation) {
        return new DrawableTransformationOptions(context).transformBitmap(transformation);
    }

    public DrawableTransformationOptions(Context context) {
        this.context = context.getApplicationContext();
    }

    public DrawableTransformationOptions fitCenter() {
        return transformBitmap(new FitCenter(context));
    }

    public DrawableTransformationOptions centerCrop() {
        return transformBitmap(new CenterCrop(context));
    }

    public DrawableTransformationOptions transformBitmap(Transformation<Bitmap> transformation) {
        return transform(new DrawableTransformation(transformation, getBitmapDrawableTransformer(),
                getGifDrawableTransformer()));
    }

    private DrawableTransformation.Transformer<?> getBitmapDrawableTransformer() {
        if (bitmapDrawableTransformer == null) {
            bitmapDrawableTransformer = new BitmapDrawableTransformer(context);
        }
        return bitmapDrawableTransformer;
    }

    private DrawableTransformation.Transformer<?> getGifDrawableTransformer() {
        if (gifDrawableTransformer == null) {
            gifDrawableTransformer = new GifDrawableTransformer(context);
        }
        return gifDrawableTransformer;
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
