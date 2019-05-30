package com.bumptech.glide.load.engine.prefill;

import android.graphics.Bitmap;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.util.Preconditions;

/**
 * A container for a put of options used to pre-fill a {@link
 * com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} with {@link Bitmap Bitmaps} of a single
 * size and configuration.
 */
public final class PreFillType {
  @VisibleForTesting static final Bitmap.Config DEFAULT_CONFIG = Bitmap.Config.RGB_565;
  private final int width;
  private final int height;
  private final Bitmap.Config config;
  private final int weight;

  /**
   * Constructor for a single type of {@link android.graphics.Bitmap}.
   *
   * @param width The width in pixels of the {@link android.graphics.Bitmap Bitmaps} to pre-fill.
   * @param height The height in pixels of the {@link android.graphics.Bitmap Bitmaps} to pre-fill.
   * @param config The {@link android.graphics.Bitmap.Config} of the {@link android.graphics.Bitmap
   *     Bitmaps} to pre-fill.
   * @param weight An integer indicating how to balance pre-filling this size and configuration of
   *     {@link android.graphics.Bitmap} against any other sizes/configurations that may be being
   *     pre-filled.
   */
  PreFillType(int width, int height, Bitmap.Config config, int weight) {
    this.config = Preconditions.checkNotNull(config, "Config must not be null");
    this.width = width;
    this.height = height;
    this.weight = weight;
  }

  /** Returns the width in pixels of the {@link android.graphics.Bitmap Bitmaps}. */
  int getWidth() {
    return width;
  }

  /** Returns the height in pixels of the {@link android.graphics.Bitmap Bitmaps}. */
  int getHeight() {
    return height;
  }

  /**
   * Returns the {@link android.graphics.Bitmap.Config} of the {@link android.graphics.Bitmap
   * Bitmaps}.
   */
  Bitmap.Config getConfig() {
    return config;
  }

  /** Returns the weight of the {@link android.graphics.Bitmap Bitmaps} of this type. */
  int getWeight() {
    return weight;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof PreFillType) {
      PreFillType other = (PreFillType) o;
      return height == other.height
          && width == other.width
          && weight == other.weight
          && config == other.config;
    }
    return false;
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
        + "width="
        + width
        + ", height="
        + height
        + ", config="
        + config
        + ", weight="
        + weight
        + '}';
  }

  /** Builder for {@link PreFillType}. */
  public static class Builder {
    private final int width;
    private final int height;

    private Bitmap.Config config;
    private int weight = 1;

    /**
     * Constructor for a builder that uses the given size as the width and height of the Bitmaps to
     * prefill.
     *
     * @param size The width and height in pixels of the Bitmaps to prefill.
     */
    public Builder(int size) {
      this(size, size);
    }

    /**
     * Constructor for a builder that uses the given dimensions as the dimensions of the Bitmaps to
     * prefill.
     *
     * @param width The width in pixels of the Bitmaps to prefill.
     * @param height The height in pixels of the Bitmaps to prefill.
     */
    public Builder(int width, int height) {
      if (width <= 0) {
        throw new IllegalArgumentException("Width must be > 0");
      }
      if (height <= 0) {
        throw new IllegalArgumentException("Height must be > 0");
      }
      this.width = width;
      this.height = height;
    }

    /**
     * Sets the {@link android.graphics.Bitmap.Config} for the Bitmaps to pre-fill.
     *
     * @param config The config to use, or null to use Glide's default.
     * @return This builder.
     */
    public Builder setConfig(@Nullable Bitmap.Config config) {
      this.config = config;
      return this;
    }

    /** Returns the current {@link android.graphics.Bitmap.Config}. */
    Bitmap.Config getConfig() {
      return config;
    }

    /**
     * Sets the weight to use to balance how many Bitmaps of this type are prefilled relative to the
     * other requested types.
     *
     * @param weight An integer indicating how to balance pre-filling this size and configuration of
     *     {@link android.graphics.Bitmap} against any other sizes/configurations that may be being
     *     pre-filled.
     * @return This builder.
     */
    public Builder setWeight(int weight) {
      if (weight <= 0) {
        throw new IllegalArgumentException("Weight must be > 0");
      }
      this.weight = weight;
      return this;
    }

    /** Returns a new {@link PreFillType}. */
    PreFillType build() {
      return new PreFillType(width, height, config, weight);
    }
  }
}
