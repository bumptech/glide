package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;

import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

/**
 * A bitmap decoder for a given resource type.
 *
 * @param <T> The type of resource this decoder can decode a {@link Bitmap} from.
 */
public interface BitmapDecoder<T> {
    /**
     * Returns a decoded bitmap for a given resource and target dimensions.
     *
     * @param resource The resource to decode, managed by the caller, no need to clean it up.
     * @param bitmapPool A bitmap pool that can be used to reuse bitmaps during the load. Any bitmaps created or
     *                   obtained from the pool other than the bitmap returned by this method should be returned to the
     *                   pool.
     * @param outWidth The target width for the returned bitmap (need not match exactly).
     * @param outHeight The target height for the returned bitmap (need not match exactly).
     * @param decodeFormat The desired configuration for the returned bitmap.
     */
    Bitmap decode(T resource, BitmapPool bitmapPool, int outWidth, int outHeight, DecodeFormat decodeFormat)
            throws Exception;

    /**
     * Returns some unique String id that distinguishes this decoder from any other decoder.
     *
     * <p>
     *     This method can return the empty string if for all practical purposes it applies no transformations to the
     *     data while loading the resource. For {@link android.graphics.Bitmap}s this would mean at a minimum doing no
     *     downsampling and also probably always producing {@link android.graphics.Bitmap}s with
     *     {@link android.graphics.Bitmap.Config#ARGB_8888} as their config.
     * </p>
     */
    String getId();
}
