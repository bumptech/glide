package com.bumptech.glide.resize.load;

import android.graphics.Bitmap;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;

/**
 * A class for performing an arbitrary transformation on a bitmap
 */
public abstract class Transformation {
    private final String id = getClass().toString();

    /**
     * Scale the image uniformly (maintaining the image's aspect ratio) so that one of the dimensions of the image
     * will be equal to the given dimension and the other will be less than the given dimension
     */
    public static Transformation CENTER_CROP = new Transformation() {
        @Override
        public Bitmap transform(Bitmap bitmap, BitmapPool pool, int outWidth, int outHeight) {
            return ImageResizer.centerCrop(pool.get(outWidth, outHeight), bitmap, outWidth, outHeight);
        }
    };

    /**
     * Scale the image so that either the width of the image matches the given width and the height of the image is
     * greater than the given height or vice versa, and then crop the larger dimension to match the given dimension.
     *
     * Does not maintain the image's aspect ratio
     */
    public static Transformation FIT_CENTER = new Transformation() {
        @Override
        public Bitmap transform(Bitmap bitmap, BitmapPool pool, int outWidth, int outHeight) {
            return ImageResizer.fitInSpace(bitmap, outWidth, outHeight);
        }
    };

    /**
     * A noop Transformation that simply returns the given bitmap
     */
    public static Transformation NONE = new Transformation() {
        @Override
        public Bitmap transform(Bitmap bitmap, BitmapPool pool, int outWidth, int outHeight) {
            return bitmap;
        }
    };

    /**
     * Transform the given bitmap. It is also acceptable to simply return the given bitmap if no transformation is
     * required.
     *
     * @param bitmap The bitmap to transform
     * @param pool A bitmap pool to obtain reused bitmaps from and release unneeded bitmaps to. It is always safe
     *             to attempt to retrieve bitmaps. However, any bitmaps released to the pool must not be referenced
     *             elsewhere or returned.
     * @param outWidth The width of the view or target the bitmap will be displayed in
     * @param outHeight The height of the view or target the bitmap will be displayed in
     * @return The transformed bitmap
     */
    public abstract Bitmap transform(Bitmap bitmap, BitmapPool pool, int outWidth, int outHeight);

    /**
     * A method to get a unique identifier for this particular transformation that can be used as part of a cache key
     *
     * @return A string that uniquely identifies this transformation from other transformations
     */
    public String getId() {
        return id;
    }
}
