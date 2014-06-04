package com.bumptech.glide.load;

import android.graphics.Bitmap;
import com.bumptech.glide.load.resource.bitmap.BitmapDecoder;

/**
 * Options for setting the value of {@link Bitmap#getConfig()} for {@link Bitmap}s returned by a {@link BitmapDecoder}.
 *
 * <p>
 *     Note - In some cases it may not be possible to obey the requested setting, not all {@link BitmapDecoder}s support
 *     setting formats and certain images may not be able to be loaded as certain configurations. Therefore this class
 *     represents a preference rather than a requirement.
 * </p>
 */
public enum DecodeFormat {
    /**
     * All bitmaps returned by the {@link BitmapDecoder} should return {@link Bitmap.Config#ARGB_8888} for
     * {@link Bitmap#getConfig()}.
     */
    ALWAYS_ARGB_8888,

    /**
     * Bitmaps decoded from image formats that support and/or use alpha (some types of PNGs, GIFs etc) should
     * return {@link Bitmap.Config#ARGB_8888} for {@link Bitmap#getConfig()}. Bitmaps decoded from formats that don't
     * support or use alpha should return {@link Bitmap.Config#RGB_565} for {@link Bitmap#getConfig()}.
     *
     */
    PREFER_RGB_565,
}
