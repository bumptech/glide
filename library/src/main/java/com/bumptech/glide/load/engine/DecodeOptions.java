package com.bumptech.glide.load.engine;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.load.Transformation;

/**
 * The standard concrete implementation of {@link com.bumptech.glide.load.engine.BaseDecodeOptions}.
 *
 * <p>
 *     Users with custom types or transformations can subclass {@link com.bumptech.glide.load.engine.BaseDecodeOptions}
 *     to include their custom options and/or transformations.
 * </p>
 *
 */
public final class DecodeOptions extends BaseDecodeOptions<DecodeOptions> {

    public static DecodeOptions fitCenter(Context context) {
        return new DecodeOptions(context).fitCenter();
    }

    public static DecodeOptions centerCrop(Context context) {
        return new DecodeOptions(context);
    }

    public static DecodeOptions transform(Context context, Transformation<Bitmap> transformation) {
        return new DecodeOptions(context).transform(transformation);
    }

    public static DecodeOptions dontTransform(Context context) {
        return new DecodeOptions(context).dontTransform();
    }

    public static DecodeOptions option(Context context, String key, Object option) {
        return new DecodeOptions(context).set(key, option);
    }

    public static DecodeOptions through(Context context, Class<?> resourceClass) {
        return new DecodeOptions(context).through(resourceClass);
    }

    public DecodeOptions(Context context) {
        super(context);
    }
}
