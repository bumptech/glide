package com.bumptech.glide.loader.transformation;

import com.bumptech.glide.resize.load.Transformation;

/**
 * A class for loading a {@link Transformation} for a particular model. This allows things like rotating the image based
 * on its exif data
 *
 * @param <T> The type of the model to be loaded
 */
public interface TransformationLoader<T> {
    /**
     * Get the {@link Transformation} for the model
     *
     * @param model The model
     * @return A new or static (if the transformation is type/model agnostic) {@link Transformation}
     */
    public Transformation getTransformation(T model);
}
