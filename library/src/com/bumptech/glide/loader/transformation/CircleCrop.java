package com.bumptech.glide.loader.transformation;

import com.bumptech.glide.resize.load.Transformation;

/**
 * Load image to exactly match the view in one dimension and then crop the image to fit the other dimension.
 *
 * @param <T> The type of the model. Can be any type since the {@link Transformation is model/type agnostic}.
 */
public class CircleCrop<T> implements TransformationLoader<T>{
    @Override
    public Transformation getTransformation(T model) {
        return Transformation.CIRCLE_CROP;
    }

    @Override
    public String getId() {
        return Transformation.CIRCLE_CROP.getId();
    }
}