package com.bumptech.glide.load.resource.bitmap;

/**
 * Indicates the algorithm to use when downsampling images.
 */
public abstract class DownsampleStrategy {

  /**
   * Downsamples so the image's dimensions are between the given dimensions and 2x the given
   * dimensions.
   */
  public static final DownsampleStrategy AT_LEAST = new DownsampleStrategy() {

    @Override
    public int getSampleSize(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      return Math.min(sourceHeight / requestedHeight, sourceWidth / requestedWidth);
    }
  };

  /**
   * Downsamples, maintaining the original aspect ratio, so that one of the image's dimensions is
   * exactly equal to the requested size and the other dimension is less than or equal to the
   * requested size.
   */
  public static final DownsampleStrategy CENTER_INSIDE = new DownsampleStrategy() {
    @Override
    public int getSampleSize(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      return AT_LEAST.getSampleSize(sourceWidth, sourceHeight, requestedWidth, requestedHeight);
    }

    @Override
    public int getDensity(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      float widthPercentage = requestedWidth / (float) sourceWidth;
      float heightPercentage = requestedHeight / (float) sourceHeight;
      return widthPercentage < heightPercentage ? sourceWidth : sourceHeight;
    }

    @Override
    public int getTargetDensity(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight, int sampleSize) {
      float widthPercentage = requestedWidth / (float) sourceWidth;
      float heightPercentage = requestedHeight / (float) sourceHeight;
      int targetDimen = widthPercentage < heightPercentage ? requestedWidth : requestedHeight;
      return targetDimen * sampleSize;
    }
  };

  /**
   * Downsamples, maintaining the original aspect ratio , so that one of the image's dimensions is
   * exactly equal to the requested size and the other dimension is greater than or equal to
   * the requested size.
   */
  public static final DownsampleStrategy CENTER_OUTSIDE = new DownsampleStrategy() {
    @Override
    public int getSampleSize(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      return AT_LEAST.getSampleSize(sourceWidth, sourceHeight, requestedWidth, requestedHeight);
    }

    @Override
    public int getDensity(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      if (sourceWidth * requestedHeight > sourceHeight * requestedHeight) {
        return sourceHeight;
      } else {
        return sourceWidth;
      }
    }

    @Override
    public int getTargetDensity(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight, int sampleSize) {

      final int targetDimen;
      if (sourceWidth * requestedHeight > sourceHeight * requestedHeight) {
        targetDimen = requestedHeight;
      } else {
        targetDimen = requestedWidth;
      }
      return targetDimen * sampleSize;
    }
  };

  /**
   * Downsamples so the image's dimensions are between 1/2 the given dimensions and the given
   * dimensions.
   */
  public static final DownsampleStrategy AT_MOST = new DownsampleStrategy() {
    @Override
    public int getSampleSize(int sourceWidth, int sourceHeight, int requestedWidth,
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

    @Override
    public int getDensity(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      return 0;
    }

    @Override
    public int getTargetDensity(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight, int sampleSize) {
      return 0;
    }
  };

  /**
   * Performs no downsampling.
   */
  public static final DownsampleStrategy NONE = new DownsampleStrategy() {
    @Override
    public int getSampleSize(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      return 1;
    }

    @Override
    public int getDensity(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight) {
      return 0;
    }

    @Override
    public int getTargetDensity(int sourceWidth, int sourceHeight, int requestedWidth,
        int requestedHeight, int sampleSize) {
      return 0;
    }
  };

  /**
   * Default strategy, currently {@link #AT_LEAST}.
   */
  public static final DownsampleStrategy DEFAULT = AT_LEAST;

  /**
   * Determine the amount of downsampling to use for a load given the dimensions of the image
   * to be downsampled and the dimensions of the view/target the image will be displayed in.
   *
   * @param sourceWidth   The width in pixels of the image to be downsampled.
   * @param sourceHeight  The height in pixels of the image to be downsampled.
   * @param requestedWidth  The width in pixels of the view/target the image will be displayed in.
   * @param requestedHeight The height in pixels of the view/target the image will be displayed in.
   * @return An integer to pass in to {@link android.graphics.BitmapFactory#decodeStream(
   * java.io.InputStream, android.graphics.Rect, android.graphics.BitmapFactory.Options)}.
   * @see android.graphics.BitmapFactory.Options#inSampleSize
   */
  public abstract int getSampleSize(int sourceWidth, int sourceHeight, int requestedWidth,
      int requestedHeight);

  /**
   * Returns an integer value for {@link android.graphics.BitmapFactory.Options#inDensity} that can
   * be used, along with {@link #getTargetDensity(int, int, int, int, int)} to scale the image
   * natively, or {@code 0} to additional avoid scaling.
   *
   * @param sourceWidth   The width in pixels of the image to be downsampled.
   * @param sourceHeight  The height in pixels of the image to be downsampled.
   * @param requestedWidth  The width in pixels of the view/target the image will be displayed in.
   * @param requestedHeight The height in pixels of the view/target the image will be displayed in.
   */
  public int getDensity(int sourceWidth, int sourceHeight, int requestedWidth,
      int requestedHeight) {
    return 0;
  }

  /**
   * Returns an integer value for {@link android.graphics.BitmapFactory.Options#inTargetDensity}
   * that can be used, along with {@link #getDensity(int, int, int, int)}} to scale the image
   * natively, or {@code 0} to additional avoid scaling.
   *
   * <p> The additional scaling will be applied by multiplying the result of
   * inTargetDensity/inDensity to to downsampled image (sourceWidth/Height divided by sampleSize)
   * </p>
   *
   * <p> Images can only be downscaled, scale factors from densities greater than 1 will be ignored.
   * </p>
   *
   * @param sourceWidth   The width in pixels of the image to be downsampled.
   * @param sourceHeight  The height in pixels of the image to be downsampled.
   * @param requestedWidth  The width in pixels of the view/target the image will be displayed in.
   * @param requestedHeight The height in pixels of the view/target the image will be displayed in.
   * @param sampleSize The sample size that will be used to downsample the image before any
   *                   additional density related scaling is applied.
   */
  public int getTargetDensity(int sourceWidth, int sourceHeight, int requestedWidth,
      int requestedHeight, int sampleSize) {
    return 0;
  }
}
