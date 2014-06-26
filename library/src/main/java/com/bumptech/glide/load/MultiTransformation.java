package com.bumptech.glide.load;

import com.bumptech.glide.load.engine.Resource;

import java.util.List;

/**
 * A transformation that applies an ordered array of one or more transformations to an image.
 */
public class MultiTransformation<T> implements Transformation<T> {
    private Transformation<T>[] transformations;
    private List<Transformation<T>> transformationList;
    private String id;

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
        Resource<T> previous = resource;

        if (transformations != null) {
            for (Transformation<T> transformation : transformations) {
                Resource<T> transformed = transformation.transform(previous, outWidth, outHeight);
                if (transformed != previous && previous != resource && previous != null) {
                    previous.recycle();
                }
                previous = transformed;
            }
        } else {
            for (Transformation<T> transformation : transformationList) {
                 Resource<T> transformed = transformation.transform(previous, outWidth, outHeight);
                if (transformed != previous && previous != resource && previous != null) {
                    previous.recycle();
                }
                previous = transformed;
            }

        }
        return previous;
    }

    @Override
    public String getId() {
        if (id == null) {
            StringBuilder sb = new StringBuilder();
            if (transformations != null) {
                for (Transformation transformation : transformations) {
                    sb.append(transformation.getId());
                }
            } else {
                for (Transformation transformation : transformationList) {
                    sb.append(transformation.getId());
                }
            }
            id = sb.toString();
        }
        return id;
    }
}
