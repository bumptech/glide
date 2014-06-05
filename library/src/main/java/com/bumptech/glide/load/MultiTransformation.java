package com.bumptech.glide.load;

import com.bumptech.glide.Resource;

import java.util.List;

/**
 * A transformation that applies an ordered array of one or more transformations to an image.
 */
public class MultiTransformation<T> implements Transformation<T> {
    private Transformation<T>[] transformations;
    private List<Transformation<T>> transformationList;

    public MultiTransformation(Transformation<T>... transformations) {
        if (transformations.length < 1) {
            throw new IllegalArgumentException("MultiTransformation must contain at least one Transformation");
        }
        this.transformations = transformations;
    }

    public MultiTransformation(List<Transformation<T>> transformationList) {
        if (transformationList.size() < 1) {
            throw new IllegalArgumentException("MultiTransformation must contain at least one Transformation");
        }
        this.transformationList = transformationList;

    }

    @Override
    public Resource<T> transform(Resource<T> resource, int outWidth, int outHeight) {
        // Set current to null so we don't recycle our original bitmap. Instead rely on the caller of this method to do
        // so.
        Resource<T> current = null;

        if (transformations != null) {
            for (Transformation<T> transformation : transformations) {
                current = transform(current, transformation, outWidth, outHeight);
            }
        } else {
            for (Transformation<T> transformation : transformationList) {
                current = transform(current, transformation, outWidth, outHeight);
            }

        }
        return current;
    }

    private Resource<T> transform(Resource<T> current, Transformation<T> transformation, int outWidth,
            int outHeight) {
        Resource<T> transformed = transformation.transform(current, outWidth, outHeight);
        if (current != null && current != transformed) {
            current.recycle();
        }

        return transformed;
    }

    @Override
    public String getId() {
        StringBuilder sb = new StringBuilder();
        for (Transformation transformation : transformations) {
            sb.append(transformation.getId());
        }
        return sb.toString();
    }
}
