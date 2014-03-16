package com.bumptech.glide.resize.load;

import android.graphics.Bitmap;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;

import java.io.IOException;

/**
 * A bitmap decoder for a given resource type.
 *
 * @param <T> The type of resource this decoder can decode a {@link Bitmap} from.
 */
public interface BitmapDecoder<T> {
    /**
     * Returns a decoded bitmap for a given resource and target dimensions.
     *
     * @param resource The resource to decode.
     * @param bitmapPool A bitmap pool that can be used to reuse bitmaps during the load. Any bitmaps created or
     *                   obtained from the pool other than the bitmap returned by this method should be returned to the
     *                   pool.
     * @param outWidth The target width for the returned bitmap (need not match exactly).
     * @param outHeight The target height for the returne bitmap (need not match exactly).
     */
    public Bitmap decode(T resource, BitmapPool bitmapPool, int outWidth, int outHeight) throws Exception;

    /**
     * Returns some unique String id that distinguishes this decoder from any other decoder.
     */
    public String getId();
}
