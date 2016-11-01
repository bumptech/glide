package com.bumptech.glide.load.resource.bitmap;

import com.bumptech.glide.util.Synthetic;

/**
 * Indicates the algorithm to use when downsampling images.
 */
public abstract class DownsampleStrategy {

  /**
   * Scales, maintaining the original aspect ratio, so that one of the image's dimensions is
   * exactly equal to the requested size and the other dimension is less than or equal to the
   * requested size.
   *
   * <p>This method will upscale if the requested width and height are greater than the source width
   * and height. To avoid upscaling, use {@link #AT_LEAST}, {@link #AT_MOST} or
   * {@link #CENTER_INSIDE}.
   *
   * <p>On pre-KitKat devices, this is equivalent to {@link #AT_MOST} because only power of
   * two downsampling can be used.
   */
  public static final DownsampleStrategy FIT_CENTER = new FitCenter();

  /**
   * Scales, maintaining the original aspect ratio, so that one of the image's dimensions is
   * exactly equal to the requested size and the other dimension is greater than or equal to
   * the requested size.
   *
   * <p>This method will upscale if the requested width and height are greater than the source width
   * and height. To avoid upscaling, use {@link #AT_LEAST}, {@link #AT_MOST},
   * or {@link #CENTER_INSIDE}.
   *
   * <p>On pre-KitKat devices, this is equivalent to {@link #AT_LEAST} because only power of
   * two downsampling can be used.
   */
  public static final DownsampleStrategy CENTER_OUTSIDE = new CenterOutside();

  /**
   * Downsamples so the image's smallest dimension is between the given dimensions and 2x the given
   * dimensions, with no size restrictions on the image's largest dimension.
   */
  public static final DownsampleStrategy AT_LEAST = new AtLeast();

  /**
   * Downsamples so the image's largest dimension is between 1/2 the given dimensions and the given
   * dimensions, with no restrictions on the image's smallest dimension.
   */
  public static final DownsampleStrategy AT_MOST = new AtMost();

  /**
   * Returns the original image if it is smaller than the target, otherwise it will be downscaled
   * maintaining its original aspect ratio, so that one of the image's dimensions is exactly equal
   * to the requested size and the other is less or equal than the requested size.
   *
   * <p>This method will not upscale.</p>
   */
  public static final DownsampleStrategy CENTER_INSIDE = new CenterInside();

  /**
   * Performs no downsampling or scaling.
   */
  public static final DownsampleStrategy NONE = new None();

  /**
   * Default strategy, currently {@link #AT_LEAST}.
   */
  public static final DownsampleStrategy DEFAULT = AT_LEAST;

  /**
   * Returns a float (0, +infinity) indicating a scale factor to apply to the source
   * width and height when displayed in the requested width and height.
   *
   * <p>The returned scale factor will be split into a power of two sample size applied via
   * {@link android.graphics.BitmapFactory.Options#inSampleSize} and a float scale factor applied
   * after downsampling via {@link android.graphics.BitmapFactory.Options#inTargetDensity} and
   * {@link android.graphics.BitmapFactory.Options#inDensity}. Because of rounding errors the scale
   * factor may not be applied precisely.
   *
   * <p>The float scaling factor will only be applied on KitKat+. Prior to KitKat, only the power
   * of two downsampling will be applied.
   *
   * @param sourceWidth   The width in pixels of the image to be downsampled.
   * @param sourceHeight  The height in pixels of the image to be downsampled.
   * @param requestedWidth  The width in pixels of the view/target the image will be displayed in.
   * @param requestedHeight The height in pixels of the view/target the image will be displayed in.
   */
  public abstract float getScaleFactor(int sourceWidth, int sourceHeight, int requestedWidth,
      int requestedHeight);

  /**
   * Returns a non-null {@link SampleSizeRounding} to use to resolve rounding errors and conflicts
   * between scaling for the width and the height of the image.
   *
   * @param sourceWidth   The width in pixels of the image to be downsampled.
   * @param sourceHeight  The height in pixels of the image to be downsampled.
   * @param requestedWidth  The width in pixels of the view/target the image will be displayed in.
   * @param requestedHeight The height in pixels of the view/target the image will be displayed in.
   */
  public abstract SampleSizeRounding getSampleSizeRounding(int sourceWidth, int sourceHeight,
      int requestedWidth, int requestedHeight);

  private static class FitCenter extends DownsampleStrategy {

    @Synthetic
    FitCenter() { }

    @Override
    public float getScaleFactor(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      float widthPercentage = requestedWidth / (float) sourceWidth;
      float heightPercentage = requestedHeight / (float) sourceHeight;
      return Math.min(widthPercentage, heightPercentage);
    }

    @Override
    public SampleSizeRounding getSampleSizeRounding(int sourceWidth, int sourceHeight,
        int requestedWidth, int requestedHeight) {
      return SampleSizeRounding.QUALITY;
    }
  }

  private static class CenterOutside extends DownsampleStrategy {

    @Synthetic
    CenterOutside() { }

    @Override
    public float getScaleFactor(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      float widthPercentage = requestedWidth / (float) sourceWidth;
      float heightPercentage = requestedHeight / (float) sourceHeight;
      return Math.max(widthPercentage, heightPercentage);
    }

    @Override
    public SampleSizeRounding getSampleSizeRounding(int sourceWidth, int sourceHeight,
        int requestedWidth, int requestedHeight) {
      return SampleSizeRounding.QUALITY;
    }
  }

  private static class AtLeast extends DownsampleStrategy {

    @Synthetic
    AtLeast() { }

    @Override
    public float getScaleFactor(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      int minIntegerFactor = Math.min(sourceHeight / requestedHeight, sourceWidth / requestedWidth);
      return minIntegerFactor == 0 ? 1f : 1f / Integer.highestOneBit(minIntegerFactor);
    }

    @Override
    public SampleSizeRounding getSampleSizeRounding(int sourceWidth, int sourceHeight,
        int requestedWidth, int requestedHeight) {
      return SampleSizeRounding.QUALITY;
    }
  }

  private static class AtMost extends DownsampleStrategy {

    @Synthetic
    AtMost() { }

    @Override
    public float getScaleFactor(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      int maxIntegerFactor = (int) Math.ceil(Math.max(sourceHeight / (float) requestedHeight,
              sourceWidth / (float) requestedWidth));
      int lesserOrEqualSampleSize = Math.max(1, Integer.highestOneBit(maxIntegerFactor));
      int greaterOrEqualSampleSize =
          lesserOrEqualSampleSize << (lesserOrEqualSampleSize < maxIntegerFactor ? 1 : 0);
      return 1f / greaterOrEqualSampleSize;
    }

    @Override
    public SampleSizeRounding getSampleSizeRounding(int sourceWidth, int sourceHeight,
        int requestedWidth, int requestedHeight) {
      return SampleSizeRounding.MEMORY;
    }
  }

  private static class None extends DownsampleStrategy {

    @Synthetic
    None() { }

    @Override
    public float getScaleFactor(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      return 1f;
    }

    @Override
    public SampleSizeRounding getSampleSizeRounding(int sourceWidth, int sourceHeight,
        int requestedWidth, int requestedHeight) {
      return SampleSizeRounding.QUALITY;
    }
  }

  private static class CenterInside extends DownsampleStrategy {

    @Synthetic
    CenterInside() { }

    @Override
    public float getScaleFactor(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {

      return Math.min(1.f,
          FIT_CENTER.getScaleFactor(sourceWidth, sourceHeight, requestedWidth, requestedHeight));
    }

    @Override
    public SampleSizeRounding getSampleSizeRounding(int sourceWidth, int sourceHeight,
        int requestedWidth, int requestedHeight) {
      return SampleSizeRounding.QUALITY;
    }
  }

  /**
   * Indicates whether to prefer to prefer downsampling or scaling to prefer lower memory usage
   * or higher quality.
   */
  public enum SampleSizeRounding {
    /**
     * Prefer to round the sample size up so that the image is downsampled to smaller than the
     * requested size to use less memory.
     */
    MEMORY,
    /**
     * Prefer to round the sample size down so that the image is downsampled to larger than the
     * requested size to maintain quality at the expense of extra memory usage.
     */
    QUALITY,
  }
}
