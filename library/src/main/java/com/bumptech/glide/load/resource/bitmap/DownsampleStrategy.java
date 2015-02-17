package com.bumptech.glide.load.resource.bitmap;

/**
 * Indicates the algorithm to use when downsampling images.
 */
public interface DownsampleStrategy {

  /**
   * Downsamples so the image's dimensions are between the given dimensions and 2x the given
   * dimensions.
   */
  DownsampleStrategy AT_LEAST = new DownsampleStrategy() {

    @Override
    public int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight) {
      return Math.min(inHeight / outHeight, inWidth / outWidth);
    }
  };

  /**
   * Downsamples so the image's dimensions are between 1/2 the given dimensions and the given
   * dimensions.
   */
  DownsampleStrategy AT_MOST = new DownsampleStrategy() {
    @Override
    public int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight) {
      return Math.max(inHeight / outHeight, inWidth / outWidth);
    }
  };

  /**
   * Performs no downsampling.
   */
  DownsampleStrategy NONE = new DownsampleStrategy() {
    @Override
    public int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight) {
      return 1;
    }
  };

  /**
   * Default strategy, currently {@link #AT_LEAST}.
   */
  DownsampleStrategy DEFAULT = AT_LEAST;

  /**
   * Determine the amount of downsampling to use for a load given the dimensions of the image
   * to be
   * downsampled and the dimensions of the view/target the image will be displayed in.
   *
   * @param inWidth   The width in pixels of the image to be downsampled.
   * @param inHeight  The height in piexels of the image to be downsampled.
   * @param outWidth  The width in pixels of the view/target the image will be displayed in.
   * @param outHeight The height in pixels of the view/target the imag will be displayed in.
   * @return An integer to pass in to {@link android.graphics.BitmapFactory#decodeStream(
   * java.io.InputStream, android.graphics.Rect, android.graphics.BitmapFactory.Options)}.
   * @see android.graphics.BitmapFactory.Options#inSampleSize
   */
  int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight);
}
