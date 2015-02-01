package com.bumptech.glide.load.engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawableTransformation;
import com.bumptech.glide.util.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * A base class for setting decode options that includes default overridable implementations for Glide's standard
 * options and transformations.
 *
 * @param <CHILD> The specific subclass used as part of the curiously repeating template pattern.
 */
public abstract class BaseDecodeOptions<CHILD extends BaseDecodeOptions<CHILD>> implements Cloneable {
    private final Context context;

    private Map<String, Object> options = new HashMap<String, Object>();
    private Map<Class<?>, Transformation<?>> transformations = new HashMap<Class<?>, Transformation<?>>();
    private boolean isTransformationSet;

    public BaseDecodeOptions(Context context) {
        this.context = context.getApplicationContext();
    }

    public final <T> CHILD transform(Class<T> resourceClass, Transformation<T> transformation) {
        Preconditions.checkNotNull(resourceClass);
        Preconditions.checkNotNull(transformation);
        isTransformationSet = true;
        transformations.put(resourceClass, transformation);
        return self();
    }

    public final CHILD dontTransform() {
        isTransformationSet = false;
        transformations.clear();
        return self();
    }

    public final CHILD set(String key, Object option) {
        options.put(key, option);
        return self();
    }

    public final CHILD apply(BaseDecodeOptions<?> other) {
        transformations.putAll(other.transformations);
        options.putAll(other.options);
        return self();
    }

    public final boolean isTransformationSet() {
        return isTransformationSet;
    }

    public CHILD centerCrop() {
        return transform(new CenterCrop(context));
    }

    public CHILD fitCenter() {
        return transform(new FitCenter(context));
    }

    public CHILD transform(Transformation<Bitmap> transformation) {
        transform(Bitmap.class, transformation);
        // TODO: remove BitmapDrawable decoder and this transformation.
        transform(BitmapDrawable.class, new BitmapDrawableTransformation(context, transformation));
        transform(GifDrawable.class, new GifDrawableTransformation(context, transformation));
        return self();
    }

    @SuppressWarnings("unchecked")
    private CHILD self() {
        return (CHILD) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final CHILD clone() throws CloneNotSupportedException {
        try {
            BaseDecodeOptions<CHILD> result = (BaseDecodeOptions<CHILD>) super.clone();
            result.options = new HashMap<String, Object>();
            result.options.putAll(options);
            result.transformations =  new HashMap<Class<?>, Transformation<?>>();
            result.transformations.putAll(transformations);
            return (CHILD) result;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    final <T> Transformation<T> getTransformation(Class<T> resourceClass) {
        Transformation<T> result = (Transformation<T>) transformations.get(resourceClass);
        if (result == null) {
            if (!transformations.isEmpty()) {
                throw new IllegalArgumentException("Missing transformation for " + resourceClass);
            } else {
                return UnitTransformation.get();
            }
        }
        return result;
    }

    final Map<String, Object> getOptions() {
        return options;
    }
}
