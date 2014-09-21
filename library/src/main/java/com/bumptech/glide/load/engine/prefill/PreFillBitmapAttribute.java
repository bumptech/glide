package com.bumptech.glide.load.engine.prefill;

import android.graphics.Bitmap;

/**
 * A container for a set of options used to pre-fill a {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool}
 * with {@link Bitmap Bitmaps} of a single size and configuration.
 */
public final class PreFillBitmapAttribute {
    // Visible for testing.
    static final Bitmap.Config DEFAULT_CONFIG = Bitmap.Config.RGB_565;
    private final int width;
    private final int height;
    private final Bitmap.Config config;
    private final int weight;

    /**
     * Constructor for a single type of {@link android.graphics.Bitmap}.
     *
     * @see #PreFillBitmapAttribute(int, int, int)
     * @see #PreFillBitmapAttribute(int, int, android.graphics.Bitmap.Config, int)
     *
     * @param width The width in pixels of the {@link android.graphics.Bitmap} to pre-fill.
     * @param height The height in pixels of the {@link android.graphics.Bitmap} to pre-fill.
     */
    public PreFillBitmapAttribute(int width, int height) {
        this(width, height, 1);
    }

    /**
     * Constructor for a single type of {@link android.graphics.Bitmap}.
     *
     * @see #PreFillBitmapAttribute(int, int)
     * @see #PreFillBitmapAttribute(int, int, android.graphics.Bitmap.Config, int)
     *
     * @param width The width in pixels of the {@link android.graphics.Bitmap} to pre-fill.
     * @param height The height in pixels of the {@link android.graphics.Bitmap} to pre-fill.
     * @param weight An integer indicating how to balance pre-filling this size and configuration of
     * {@link android.graphics.Bitmap} against any other sizes/configurations that may be being pre-filled.
     */
    public PreFillBitmapAttribute(int width, int height, int weight) {
        this(width, height, DEFAULT_CONFIG, weight);
    }

    /**
     * Constructor for a single type of {@link android.graphics.Bitmap}.
     *
     * @see #PreFillBitmapAttribute(int, int)
     * @see #PreFillBitmapAttribute(int, int, int)
     *
     * @param width The width in pixels of the {@link android.graphics.Bitmap Bitmaps} to
     *              pre-fill.
     * @param height The height in pixels of the {@link android.graphics.Bitmap Bitmaps} to
     *               pre-fill.
     * @param config The {@link android.graphics.Bitmap.Config} of the {@link android.graphics.Bitmap Bitmaps} to
     *               pre-fill.
     * @param weight An integer indicating how to balance pre-filling this size and configuration of
     * {@link android.graphics.Bitmap} against any other sizes/configurations that may be being pre-filled.
     */
    public PreFillBitmapAttribute(int width, int height, Bitmap.Config config, int weight) {
        this.width = width;
        this.height = height;
        this.config = config;
        this.weight = weight;
    }

    /**
     * Returns the width in pixels of the {@link android.graphics.Bitmap Bitmaps}.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height in pixels of the {@link android.graphics.Bitmap Bitmaps}.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the {@link android.graphics.Bitmap.Config} of the {@link android.graphics.Bitmap Bitmaps}.
     */
    public Bitmap.Config getConfig() {
        return config;
    }

    /**
     * Returns the weight of the {@link android.graphics.Bitmap Bitmaps} of this type.
     */
    public int getWeight() {
        return weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PreFillBitmapAttribute size = (PreFillBitmapAttribute) o;

        return height == size.height
                && weight == size.weight
                && width == size.width
                && config == size.config;
    }

    @Override
    public int hashCode() {
        int result = width;
        result = 31 * result + height;
        result = 31 * result + config.hashCode();
        result = 31 * result + weight;
        return result;
    }

    @Override
    public String toString() {
        return "PreFillSize{"
                + "width=" + width
                + ", height=" + height
                + ", config=" + config
                + ", weight=" + weight
                + '}';
    }
}
