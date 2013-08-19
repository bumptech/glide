package com.bumptech.glide.loader.transformation;

import android.graphics.Bitmap;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.resize.load.Transformation;

/**
 * A transformation that applies an ordered array of one or more transformations to an image
 */
public class MultiTransformation extends Transformation {
    private final Transformation[] transformations;

    public MultiTransformation(Transformation... transformations) {
        if (transformations.length < 1) {
            throw new IllegalArgumentException("MultiTransformation must contain at least one Transformation");
        }
        this.transformations = transformations;
    }

    @Override
    public Bitmap transform(Bitmap bitmap, BitmapPool pool, int outWidth, int outHeight) {
        Bitmap current = null; //we don't want to recycle the original image since that
                               //will be done by the ImageResizer
        Bitmap transformed;
        for (Transformation transformation : transformations) {
            transformed = transformation.transform(bitmap, pool, outWidth, outHeight);
            if (current != null && current != transformed) {
                pool.put(current);
            }

            current = transformed;
        }
        return current;
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
