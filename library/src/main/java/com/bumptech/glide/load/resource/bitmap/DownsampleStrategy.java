package com.bumptech.glide.load.resource.bitmap;

/**
 * Indicates the algorithm to use when downsampling images.
 */
public abstract class DownsampleStrategy {

  /**
   * Downsamples so the image's smallest dimension is between the given dimensions and 2x the given
   * dimensions, with no size restrictions on the image's largest dimension.
   */
  public static final DownsampleStrategy AT_LEAST = new DownsampleStrategy() {

    @Override
    public float getScaleFactor(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      return Integer.highestOneBit(
          Math.min(sourceHeight / requestedHeight, sourceWidth / requestedWidth));
    }
  };

  /**
   * Downsamples, maintaining the original aspect ratio, so that one of the image's dimensions is
   * exactly equal to the requested size and the other dimension is less than or equal to the
   * requested size.
   */
  public static final DownsampleStrategy CENTER_INSIDE = new DownsampleStrategy() {
    @Override
    public float getScaleFactor(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      float widthPercentage = requestedWidth / (float) sourceWidth;
      float heightPercentage = requestedHeight / (float) sourceHeight;
      return Math.min(widthPercentage, heightPercentage);
    }
  };

  /**
   * Downsamples, maintaining the original aspect ratio, so that one of the image's dimensions is
   * exactly equal to the requested size and the other dimension is greater than or equal to
   * the requested size.
   */
  public static final DownsampleStrategy CENTER_OUTSIDE = new DownsampleStrategy() {
    @Override
    public float getScaleFactor(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      float widthPercentage = requestedWidth / (float) sourceWidth;
      float heightPercentage = requestedHeight / (float) sourceHeight;
      return Math.max(widthPercentage, heightPercentage);
    }
  };

  /**
   * Downsamples so the image's largest dimension is between 1/2 the given dimensions and the given
   * dimensions, with no restrictions on the image's smallest dimension.
   */
  public static final DownsampleStrategy AT_MOST = new DownsampleStrategy() {
    @Override
    public float getScaleFactor(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      if (sourceWidth == requestedWidth && sourceHeight == requestedHeight) {
        return 1;
      } else {
        int maxMultiplier = (int) Math.ceil(Math.max(sourceHeight / (float) requestedHeight,
            sourceWidth / (float) requestedWidth));
        if (maxMultiplier % 2 == 0) {
          return maxMultiplier;
        } else {
          return Integer.highestOneBit(maxMultiplier) << 1;
        }
      }
    }
  };

  /**
   * Performs no downsampling.
   */
  public static final DownsampleStrategy NONE = new DownsampleStrategy() {
    @Override
    public float getScaleFactor(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      return 1f;
    }
  };

  /**
   * Default strategy, currently {@link #AT_LEAST}.
   */
  public static final DownsampleStrategy DEFAULT = AT_LEAST;

  /**
   * Returns a float between 0 and +infinity indicating a scale factor to apply to the source
   * width and height when displayed in the requested width and height.
   *
   * <p>The returned scale factor will be split into a power of two sample size applied via
   * {@link android.graphics.BitmapFactory.Options#inSampleSize} and a float scale factor applied
   * after downsampling via {@link android.graphics.BitmapFactory.Options#inTargetDensity} and
   * {@link android.graphics.BitmapFactory.Options#inDensity}. Because of rounding errors the scale
   * factor may not be applied precisely.
   *
   * @param sourceWidth   The width in pixels of the image to be downsampled.
   * @param sourceHeight  The height in pixels of the image to be downsampled.
   * @param requestedWidth  The width in pixels of the view/target the image will be displayed in.
   * @param requestedHeight The height in pixels of the view/target the image will be displayed in.
   */
  public abstract float getScaleFactor(int sourceWidth, int sourceHeight, int requestedWidth,
      int requestedHeight);
}
