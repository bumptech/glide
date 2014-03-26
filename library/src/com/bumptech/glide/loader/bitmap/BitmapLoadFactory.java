package com.bumptech.glide.loader.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.resize.BitmapLoad;

/**
 * An interface for providing a {@link BitmapLoad} to load a bitmap for a given model, width, and height.
 *
 * @param <T> The type of the model to return a {@link BitmapLoad} for.
 */
public interface BitmapLoadFactory<T> {
    /**
     * Returns a {@link BitmapLoad} that can fetch and decode a {@link Bitmap} for a given model.
     *
     * @param model The model representing the specific image.
     * @param width The target width for the decoded {@link Bitmap}.
     * @param height The target height for the decoded {@link Bitmap}.
     */
    public BitmapLoad getLoadTask(T model, int width, int height);
}
