package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.Transformation;

/**
 * Scale the image uniformly (maintaining the image's aspect ratio) so that one of the dimensions of the image
 * will be equal to the given dimension and the other will be less than the given dimension
 */
public class FitCenter implements Transformation<Bitmap> {
    private BitmapPool pool;

    public FitCenter(BitmapPool pool) {
        this.pool = pool;
    }

    @Override
    public Resource<Bitmap> transform(Resource<Bitmap> resource, int outWidth, int outHeight) {
        if (outWidth <= 0 || outHeight <= 0) {
            throw new IllegalArgumentException("Cannot fit center image to within width=" + outWidth + " or height="
                    + outHeight);
        }
        Bitmap transformed = TransformationUtils.fitCenter(resource.get(), pool, outWidth, outHeight);
        if (transformed == resource.get()) {
            return resource;
        } else {
            return new BitmapResource(transformed, pool);
        }
    }

    @Override
    public String getId() {
        return "FitCenter.com.bumptech.glide.load.Transformation";
    }
}


