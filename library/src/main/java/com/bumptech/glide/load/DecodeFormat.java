package com.bumptech.glide.load;

/**
 * Options for setting the value of {@link android.graphics.Bitmap#getConfig()} for {@link android.graphics.Bitmap}s
 * returned by a {@link com.bumptech.glide.load.resource.bitmap.BitmapDecoder}.
 *
 * <p>
 *     Note - In some cases it may not be possible to obey the requested setting, not all
 *     {@link com.bumptech.glide.load.resource.bitmap.BitmapDecoder}s support setting formats and certain images may
 *     not be able to be loaded as certain configurations. Therefore this class represents a preference rather than a
 *     requirement.
 * </p>
 */
public enum DecodeFormat {
    /**
     * All bitmaps returned by the {@link com.bumptech.glide.load.resource.bitmap.BitmapDecoder} should return
     * {@link android.graphics.Bitmap.Config#ARGB_8888} for {@link android.graphics.Bitmap#getConfig()}.
     *
     * @deprecated Use the equivalent but less misleadingly named {@link #PREFER_ARGB_8888}. Scheduled to be removed
     * in Glide 4.0
     */
    @Deprecated
    ALWAYS_ARGB_8888,

    /**
     * Bitmaps decoded from most image formats (other than GIFs with hidden configs), will be decoded with the
     * ARGB_8888 config.
     *
     * <p>
     *     {@link android.graphics.BitmapFactory} does not allow us to guarantee that all returned Bitmaps will
     *     be of a requested config without resorting to expensive copying. As a result, this is a preference only.
     *     Most GIFs, for example, will still produce {@link android.graphics.Bitmap}s with null
     *     {@link android.graphics.Bitmap.Config}s.
     * </p>
     */
    PREFER_ARGB_8888,

    /**
     * Bitmaps decoded from image formats that support and/or use alpha (some types of PNGs, GIFs etc) should
     * return {@link android.graphics.Bitmap.Config#ARGB_8888} for {@link android.graphics.Bitmap#getConfig()}. Bitmaps
     * decoded from formats that don't support or use alpha should return
     * {@link android.graphics.Bitmap.Config#RGB_565} for {@link android.graphics.Bitmap#getConfig()}.
     *
     */
    PREFER_RGB_565;

    /** The default value for DecodeFormat. */
    public static final DecodeFormat DEFAULT = PREFER_RGB_565;
}
