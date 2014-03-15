package com.bumptech.glide.resize;

import android.graphics.Bitmap;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;

/**
 * An interface for loading a bitmap.
 */
public interface BitmapLoadTask {
    /**
     * Returns some unique id representing the particular image being loaded (e.g. the image url) and any options
     * that may be being used to transform the image (downsampling, rotation etc.) suitable for use as a cache key.
     */
    public String getId();

    /**
     * Cancels any in progress load.
     */
    public void cancel();

    /**
     * Returns a decoded bitmap.
     *
     * @param bitmapPool A bitmap pool that can be used to reuse bitmaps during the load. Any bitmaps generated during
     *                   this load other than the one returned should be returned to the pool.
     * @throws Exception
     */
    public Bitmap load(BitmapPool bitmapPool) throws Exception;
}
