package com.bumptech.glide.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.bumptech.glide.ListPreloader;

/**
 * A {@link com.bumptech.glide.ListPreloader.PreloadSizeProvider} with a fixed width and height.
 *
 * @param <T> The type of the model the size should be provided for.
 */
public class FixedPreloadSizeProvider<T> implements ListPreloader.PreloadSizeProvider<T> {

  private final int[] size;

  /**
   * Constructor for a PreloadSizeProvider with a fixed size.
   *
   * @param width  The width of the preload size in pixels.
   * @param height The height of the preload size in pixels.
   */
  public FixedPreloadSizeProvider(int width, int height) {
    this.size = new int[] { width, height };
  }

  @Nullable
  @Override
  public int[] getPreloadSize(@NonNull T item, int adapterPosition, int itemPosition) {
    return size;
  }
}
