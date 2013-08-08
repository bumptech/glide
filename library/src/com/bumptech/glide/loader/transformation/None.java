package com.bumptech.glide.loader.transformation;

import com.bumptech.glide.resize.load.Transformation;

/**
 * A loader that will always return a noop {@link Transformation} that does not change a bitmap.
 *
 * @param <T> The model type, can be anything since the {@link Transformation} is type agnostic
 */
public class None<T> implements TransformationLoader<T> {
    @Override
    public Transformation getTransformation(T model) {
        return Transformation.NONE;
    }
}
