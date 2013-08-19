package com.bumptech.glide.loader.transformation;

import com.bumptech.glide.resize.load.Transformation;

import java.util.Arrays;
import java.util.List;

/**
 * A TransformationLoader that uses an ordered list of one or more transformation loaders to produce
 * a single transformation that applies each of the {@link Transformation}s produced by the loaders
 * in order.
 */
public class MultiTransformationLoader<T> implements TransformationLoader<T> {
    private final List<TransformationLoader<T>> transformationLoaders;

    @SuppressWarnings("unused")
    public MultiTransformationLoader(TransformationLoader<T>... transformationLoaders) {
        this(Arrays.asList(transformationLoaders));
    }

    public MultiTransformationLoader(List<TransformationLoader<T>> transformationLoaders) {
        if (transformationLoaders.size() < 1) {
            throw new IllegalArgumentException("MultiTransformationLoader must contain at least one " +
                    "TransformationLoader");
        }
        this.transformationLoaders = transformationLoaders;
    }

    @Override
    public Transformation getTransformation(T model) {
        int num = transformationLoaders.size();
        Transformation[] transformations = new Transformation[num];
        for (int i = 0; i < num; i++) {
            transformations[i] = transformationLoaders.get(i).getTransformation(model);
        }

        return new MultiTransformation(transformations);
    }

    @Override
    public String getId() {
        StringBuilder sb = new StringBuilder();
        for (TransformationLoader<T> transformationLoader : transformationLoaders) {
            sb.append(transformationLoader.getId());
        }
        return sb.toString();
    }
}
