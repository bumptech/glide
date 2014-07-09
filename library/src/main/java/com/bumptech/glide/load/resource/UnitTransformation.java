package com.bumptech.glide.load.resource;

import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;

/**
 * A noop Transformation that simply returns the given resource.
 */
public class UnitTransformation<T> implements Transformation<T> {
    private static final UnitTransformation TRANSFORMATION = new UnitTransformation();

    /**
     * Returns a UnitTransformation for the given type.
     *
     * @param <T> The type of the resource to be transformed.
     */
    @SuppressWarnings("unchecked")
    public static <T> UnitTransformation<T> get() {
        return TRANSFORMATION;
    }

    @Override
    public Resource<T> transform(Resource<T> resource, int outWidth, int outHeight) {
        return resource;
    }

    @Override
    public String getId() {
        return "";
    }
}
