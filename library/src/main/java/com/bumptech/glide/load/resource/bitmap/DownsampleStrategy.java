package com.bumptech.glide.load.resource.bitmap;

import android.os.Build;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.util.Synthetic;

/**
 * Indicates the algorithm to use when downsampling images.
 *
 * <p>{@code DownsampleStrategy} does not provide any guarantees about output sizes. Behavior will
 * differ depending on the {@link com.bumptech.glide.load.ResourceDecoder} using the strategy and
 * the version of Android the code runs on. Use {@code DownsampleStrategy} as an optimization to
 * improve memory efficiency only. If you need a particular size or shape output, use an {@link
 * com.bumptech.glide.load.Transformation} either instead or in addition to a {@code
 * DownsampleStrategy}.
 *
 * <p>Some differences between versions of Android and {@link
 * com.bumptech.glide.load.ResourceDecoder}s are listed below, but the list is not comprehensive
 * because {@link DownsampleStrategy} only controls its output scale value, not how that output
 * value is used.
 *
 * <p>On some versions of Android, precise scaling is not possible. In those cases, the strategies
 * can only pick between downsampling to between 1x the requested size and 2x the requested size and
 * between 0.5x the requested size and 1x the requested size because only power of two downsampling
 * is supported. To preserve the potential for a {@link com.bumptech.glide.load.Transformation} to
 * scale precisely without a loss in quality, all but {@link #AT_MOST} will prefer to downsample to
 * between 1x and 2x the requested size.
 */
// Public API.
@SuppressWarnings("WeakerAccess")
public abstract class DownsampleStrategy {

  /**
   * Downsamples so the image's smallest dimension is between the given dimensions and 2x the given
   * dimensions, with no size restrictions on the image's largest dimension.
   *
   * <p>Does not upscale if the requested dimensions are larger than the original dimensions.
   */
  public static final DownsampleStrategy AT_LEAST = new AtLeast();

  /**
   * Downsamples so the image's largest dimension is between 1/2 the given dimensions and the given
   * dimensions, with no restrictions on the image's smallest dimension.
   *
   * <p>Does not upscale if the requested dimensions are larger than the original dimensions.
   */
  public static final DownsampleStrategy AT_MOST = new AtMost();

  /**
   * Scales, maintaining the original aspect ratio, so that one of the image's dimensions is exactly
   * equal to the requested size and the other dimension is less than or equal to the requested
   * size.
   *
   * <p>This method will upscale if the requested width and height are greater than the source width
   * and height. To avoid upscaling, use {@link #AT_LEAST}, {@link #AT_MOST} or {@link
   * #CENTER_INSIDE}.
   *
   * <p>On pre-KitKat devices, {@code FIT_CENTER} will downsample by a power of two only so that one
   * of the image's dimensions is greater than or equal to the requested size. No guarantees are
   * made about the second dimensions. This is <em>NOT</em> the same as {@link #AT_LEAST} because
   * only one dimension, not both, are greater than or equal to the requested dimensions, the other
   * may be smaller.
   */
  public static final DownsampleStrategy FIT_CENTER = new FitCenter();

  /** Identical to {@link #FIT_CENTER}, but never upscales. */
  public static final DownsampleStrategy CENTER_INSIDE = new CenterInside();

  /**
   * Scales, maintaining the original aspect ratio, so that one of the image's dimensions is exactly
   * equal to the requested size and the other dimension is greater than or equal to the requested
   * size.
   *
   * <p>This method will upscale if the requested width and height are greater than the source width
   * and height. To avoid upscaling, use {@link #AT_LEAST}, {@link #AT_MOST}, or {@link
   * #CENTER_INSIDE}.
   *
   * <p>On pre-KitKat devices, {@link Downsampler} treats this as equivalent to {@link #AT_LEAST}
   * because only power of two downsampling can be used.
   */
  public static final DownsampleStrategy CENTER_OUTSIDE = new CenterOutside();

  /** Performs no downsampling or scaling. */
  public static final DownsampleStrategy NONE = new None();

  /** Default strategy, currently {@link #CENTER_OUTSIDE}. */
  public static final DownsampleStrategy DEFAULT = CENTER_OUTSIDE;

  /**
   * Indicates the {@link com.bumptech.glide.load.resource.bitmap.DownsampleStrategy} option that
   * will be used to calculate the sample size to use to downsample an image given the original and
   * target dimensions of the image.
   */
  // The exact String value here is retained to avoid breaking cache keys for images that were
  // loaded with older versions of Glide.
  public static final Option<DownsampleStrategy> OPTION =
      Option.memory(
          "com.bumptech.glide.load.resource.bitmap.Downsampler.DownsampleStrategy", DEFAULT);

  @Synthetic
  static final boolean IS_BITMAP_FACTORY_SCALING_SUPPORTED =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

  /**
   * Returns a float (0, +infinity) indicating a scale factor to apply to the source width and
   * height when displayed in the requested width and height.
   *
   * <p>The returned scale factor will be split into a power of two sample size applied via {@link
   * android.graphics.BitmapFactory.Options#inSampleSize} and a float scale factor applied after
   * downsampling via {@link android.graphics.BitmapFactory.Options#inTargetDensity} and {@link
   * android.graphics.BitmapFactory.Options#inDensity}. Because of rounding errors the scale factor
   * may not be applied precisely.
   *
   * <p>The float scaling factor will only be applied on KitKat+. Prior to KitKat, only the power of
   * two downsampling will be applied.
   *
   * @param sourceWidth The width in pixels of the image to be downsampled.
   * @param sourceHeight The height in pixels of the image to be downsampled.
   * @param requestedWidth The width in pixels of the view/target the image will be displayed in.
   * @param requestedHeight The height in pixels of the view/target the image will be displayed in.
   */
  public abstract float getScaleFactor(
      int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight);

  /**
   * Returns a non-null {@link SampleSizeRounding} to use to resolve rounding errors and conflicts
   * between scaling for the width and the height of the image.
   *
   * @param sourceWidth The width in pixels of the image to be downsampled.
   * @param sourceHeight The height in pixels of the image to be downsampled.
   * @param requestedWidth The width in pixels of the view/target the image will be displayed in.
   * @param requestedHeight The height in pixels of the view/target the image will be displayed in.
   */
  public abstract SampleSizeRounding getSampleSizeRounding(
      int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight);

  private static class FitCenter extends DownsampleStrategy {

    @Synthetic
    FitCenter() {}

    @Override
    public float getScaleFactor(
        int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight) {
      if (IS_BITMAP_FACTORY_SCALING_SUPPORTED) {
        float widthPercentage = requestedWidth / (float) sourceWidth;
        float heightPercentage = requestedHeight / (float) sourceHeight;

        return Math.min(widthPercentage, heightPercentage);
      } else {
        // Similar to AT_LEAST, but only require one dimension or the other to be >= requested
        // rather than both.
        int maxIntegerFactor =
            Math.max(sourceHeight / requestedHeight, sourceWidth / requestedWidth);
        return maxIntegerFactor == 0 ? 1f : 1f / Integer.highestOneBit(maxIntegerFactor);
      }
    }

    @Override
    public SampleSizeRounding getSampleSizeRounding(
        int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight) {
      if (IS_BITMAP_FACTORY_SCALING_SUPPORTED) {
        return SampleSizeRounding.QUALITY;
      } else {
        // TODO: This doesn't seem right, but otherwise we can skip a sample size because QUALITY
        // prefers the smaller of the width and height scale factor. MEMORY is a hack that
        // lets us prefer the larger of the two.
        return SampleSizeRounding.MEMORY;
      }
    }
  }

  private static class CenterOutside extends DownsampleStrategy {

    @Synthetic
    CenterOutside() {}

    @Override
    public float getScaleFactor(
        int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight) {
      float widthPercentage = requestedWidth / (float) sourceWidth;
      float heightPercentage = requestedHeight / (float) sourceHeight;
      return Math.max(widthPercentage, heightPercentage);
    }

    @Override
    public SampleSizeRounding getSampleSizeRounding(
        int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight) {
      return SampleSizeRounding.QUALITY;
    }
  }

  private static class AtLeast extends DownsampleStrategy {

    @Synthetic
    AtLeast() {}

    @Override
    public float getScaleFactor(
        int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight) {
      int minIntegerFactor = Math.min(sourceHeight / requestedHeight, sourceWidth / requestedWidth);
      return minIntegerFactor == 0 ? 1f : 1f / Integer.highestOneBit(minIntegerFactor);
    }

    @Override
    public SampleSizeRounding getSampleSizeRounding(
        int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight) {
      return SampleSizeRounding.QUALITY;
    }
  }

  private static class AtMost extends DownsampleStrategy {

    @Synthetic
    AtMost() {}

    @Override
    public float getScaleFactor(
        int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight) {
      int maxIntegerFactor =
          (int)
              Math.ceil(
                  Math.max(
                      sourceHeight / (float) requestedHeight,
                      sourceWidth / (float) requestedWidth));
      int lesserOrEqualSampleSize = Math.max(1, Integer.highestOneBit(maxIntegerFactor));
      int greaterOrEqualSampleSize =
          lesserOrEqualSampleSize << (lesserOrEqualSampleSize < maxIntegerFactor ? 1 : 0);
      return 1f / greaterOrEqualSampleSize;
    }

    @Override
    public SampleSizeRounding getSampleSizeRounding(
        int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight) {
      return SampleSizeRounding.MEMORY;
    }
  }

  private static class None extends DownsampleStrategy {

    @Synthetic
    None() {}

    @Override
    public float getScaleFactor(
        int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight) {
      return 1f;
    }

    @Override
    public SampleSizeRounding getSampleSizeRounding(
        int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight) {
      return SampleSizeRounding.QUALITY;
    }
  }

  private static class CenterInside extends DownsampleStrategy {

    @Synthetic
    CenterInside() {}

    @Override
    public float getScaleFactor(
        int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight) {

      return Math.min(
          1.f,
          FIT_CENTER.getScaleFactor(sourceWidth, sourceHeight, requestedWidth, requestedHeight));
    }

    @Override
    public SampleSizeRounding getSampleSizeRounding(
        int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight) {
      return getScaleFactor(sourceWidth, sourceHeight, requestedWidth, requestedHeight) == 1.f
          ? SampleSizeRounding.QUALITY
          : FIT_CENTER.getSampleSizeRounding(
              sourceWidth, sourceHeight, requestedWidth, requestedHeight);
    }
  }

  /**
   * Indicates whether to prefer to prefer downsampling or scaling to prefer lower memory usage or
   * higher quality.
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
