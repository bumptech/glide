package com.bumptech.glide.loader.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.resize.BitmapLoadTask;

/**
 * An interface for providing a {@link BitmapLoadTask} to load a bitmap for a given model, width, and height.
 *
 * @param <T> The type of the model to return a {@link BitmapLoadTask} for.
 */
public interface BitmapLoadFactory<T> {
    /**
     * Returns a {@link BitmapLoadTask} that can fetch and decode a {@link Bitmap} for a given model.
     *
     * @param model The model representing the specific image.
     * @param width The target width for the decoded {@link Bitmap}.
     * @param height The target height for the decoded {@link Bitmap}.
     */
    public BitmapLoadTask getLoadTask(T model, int width, int height);
}
