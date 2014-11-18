package com.bumptech.glide.util;

import com.bumptech.glide.ListPreloader;

import java.util.Arrays;

/**
 * A {@link com.bumptech.glide.ListPreloader.PreloadSizeProvider} with a fixed width and height.
 * @param <T> The type of the model the size should be provided for.
 */
public class FixedPreloadSizeProvider<T> implements ListPreloader.PreloadSizeProvider<T> {


    private final int[] size;

    /**
     * Create a new PreloadSizeProvider with a fixed size.
     * @param width  The width of the preload size
     * @param height The height of the preload size
     */
    public FixedPreloadSizeProvider(int width, int height) {
        this.size = new int[]{width, height};
    }

    @Override
    public int[] getPreloadSize(T item) {
        return Arrays.copyOf(this.size, this.size.length);
    }
}
