package com.bumptech.glide.load.resource.drawable;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.Preconditions;

import java.util.Arrays;

public class DrawableTransformation implements Transformation<Drawable> {
    private final Transformation<Bitmap> transformation;
    private final Transformer[] transformers;

    @Override
    public Resource<Drawable> transform(Resource<Drawable> resource, int outWidth, int outHeight) {
        Drawable toTransform = resource.get();
        Resource<Drawable> transformed = null;
        boolean handled = false;
        for (Transformer<?> transformer : transformers) {
            if (toTransform.getClass().equals(transformer.getTransformableDrawable())) {
                transformed = transform(resource, transformer, outWidth, outHeight);
                handled = true;
                break;
            }
        }
        if (!handled) {
            throw new IllegalStateException("No transformer registered for " + toTransform.getClass()
                    + ", transformers: " + Arrays.toString(transformers));
        }

        return transformed;
    }

    @SuppressWarnings({ "unchecked", "rawtypes"})
    private Resource<Drawable> transform(Resource toTransform, Transformer transformer, int width, int height) {
        return transformer.transform(toTransform, transformation, width, height);
    }

    @Override
    public String getId() {
        return transformation.getId();
    }

    public interface Transformer<T extends Drawable> {
        Resource<T> transform(Resource<T> toTransform, Transformation<Bitmap> transformation, int width, int height);
        Class<T> getTransformableDrawable();
    }

    public DrawableTransformation(Transformation<Bitmap> transformation, Transformer... transformers) {
        if (transformers.length == 0) {
            throw new IllegalArgumentException("Must provide at least one transformer");
        }
        this.transformation = Preconditions.checkNotNull(transformation, "Transformation must not be null");
        this.transformers = transformers;
    }
}
