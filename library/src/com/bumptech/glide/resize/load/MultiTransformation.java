package com.bumptech.glide.resize.load;

import android.graphics.Bitmap;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;

import java.util.List;

/**
 * A transformation that applies an ordered array of one or more transformations to an image.
 */
public class MultiTransformation extends Transformation {
    private Transformation[] transformations;
    private List<Transformation> transformationList;

    public MultiTransformation(Transformation... transformations) {
        if (transformations.length < 1) {
            throw new IllegalArgumentException("MultiTransformation must contain at least one Transformation");
        }
        this.transformations = transformations;
    }

    public MultiTransformation(List<Transformation> transformationList) {
        if (transformationList.size() < 1) {
            throw new IllegalArgumentException("MultiTransformation must contain at least one Transformation");
        }
        this.transformationList = transformationList;

    }

    @Override
    public Bitmap transform(Bitmap bitmap, BitmapPool pool, int outWidth, int outHeight) {
        // Set current to null so we don't recycle our original bitmap. Instead rely on the caller of this method to do
        // so.
        Bitmap current = null;

        if (transformations != null) {
            for (Transformation transformation : transformations) {
                current = transform(current, transformation, pool, outWidth, outHeight);
            }
        } else {
            for (Transformation transformation : transformationList) {
                current = transform(current, transformation, pool, outWidth, outHeight);
            }

        }
        return current;
    }

    private Bitmap transform(Bitmap current, Transformation transformation, BitmapPool pool, int outWidth,
            int outHeight) {
        Bitmap transformed = transformation.transform(current, pool, outWidth, outHeight);
        if (current != null && current != transformed && !pool.put(current)) {
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
