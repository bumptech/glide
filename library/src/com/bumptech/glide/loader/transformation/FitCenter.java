package com.bumptech.glide.loader.transformation;

import com.bumptech.glide.resize.load.Transformation;

/**
 * Load the image so that one dimension of the image exactly matches one of the given dimensions and the other dimension
 * of the image is smaller than or equal to the other given dimension.
 *
 * @param <T> The type of the model. Can be any type since the {@link Transformation is model/type agnostic}.
 */
public class FitCenter<T> implements TransformationLoader<T> {
    @Override
    public Transformation getTransformation(T model) {
        return Transformation.FIT_CENTER;
    }
}
