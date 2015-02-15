package com.bumptech.glide.load.resource.bitmap;

/**
 * Indicates the algorithm to use when downsampling images.
 */
public enum DownsampleStrategy {
  /**
   * Default strategy, currently
   * {@link com.bumptech.glide.load.resource.bitmap.Downsampler#AT_LEAST}.
   */
  DEFAULT(Downsampler.AT_LEAST),
  /**
   * Downsamples so the image's dimensions are between the given dimensions and 2x the given
   * dimensions.
   */
  AT_LEAST(Downsampler.AT_LEAST),
  /**
   * Downsamples so the image's dimensions are between 1/2 the given dimensions and the given
   * dimensions.
   */
  AT_MOST(Downsampler.AT_MOST),
  /**
   * Performs no downsampling.
   */
  NONE(Downsampler.NONE);

  final Downsampler downsampler;

  DownsampleStrategy(Downsampler downsampler) {
    this.downsampler = downsampler;
  }
}
