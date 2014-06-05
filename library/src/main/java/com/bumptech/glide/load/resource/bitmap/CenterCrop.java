package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.Transformation;

/**
 * Scale the image so that either the width of the image matches the given width and the height of the image is
 * greater than the given height or vice versa, and then crop the larger dimension to match the given dimension.
 *
 * Does not maintain the image's aspect ratio
 */
public class CenterCrop implements Transformation<Bitmap> {
    private BitmapPool pool;

    public CenterCrop(BitmapPool pool) {
        this.pool = pool;
    }

    @Override
    public Resource<Bitmap> transform(Resource<Bitmap> resource, int outWidth, int outHeight) {
        if (outWidth <= 0 || outHeight <= 0) {
            throw new IllegalArgumentException("Cannot center crop image to width=" + outWidth + " and height="
                    + outHeight);
        }

        final Bitmap toReuse = pool.get(outWidth, outHeight, resource.get().getConfig());
        Bitmap transformed = TransformationUtils.centerCrop(toReuse, resource.get(), outWidth, outHeight);
        if (toReuse != null && toReuse != transformed && !pool.put(toReuse)) {
            toReuse.recycle();
        }

        if (transformed == resource.get()) {
            return resource;
        } else {
            return new BitmapResource(transformed, pool);
        }
    }

    @Override
    public String getId() {
        return "CenterCrop.com.bumptech.glide.load.Transformation";
    }
}
