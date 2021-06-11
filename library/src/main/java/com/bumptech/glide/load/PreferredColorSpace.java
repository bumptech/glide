package com.bumptech.glide.load;

/**
 * Glide's supported handling of color spaces on Android O+, defaults to null.
 *
 * <p>On Android O, Glide will always request SRGB and will ignore this option if set. A bug on
 * Android O prevents P3 images from being compressed correctly and can result in color distortion.
 * We may eventually work around this in Glide if sufficient demand arises, but doing so will
 * require a memory intensive conversion to SRGB prior to compressing Bitmaps to Glide's disk cache.
 * This work around would also work only for Glide's compression, not for any compression that a
 * caller performs on a Bitmap returned by Glide.
 *
 * <p>On Android P+, Glide supports SRGB and display P3. However, if display p3 is requested, we
 * will still decode to SRGB unless {@link android.graphics.BitmapFactory.Options#outColorSpace} is
 * also {@link android.graphics.ColorSpace.Named#DISPLAY_P3}. Preferring P3 for SRGB images adds
 * unnecessary CPU work to convert back and forth between the color spaces at decode time.
 *
 * <p>Using {@link #DISPLAY_P3} is wasteful if either the screen or the renderer do not support P3.
 * Currently Glide does not attempt to detect whether or not this support is present. Do not use
 * {@link #DISPLAY_P3} thinking that you're going to get higher quality by default. Only use {@link
 * #DISPLAY_P3} if you're confident you understand color spaces, your application is working with a
 * display that supports wide gamut and you've set the appropriate options to render wide gamut
 * colors. If you've missed one or more of these steps, {@link #DISPLAY_P3} can lead to poor color
 * quality and washed out looking images. When in doubt, always use {@link #SRGB}, which is Glide's
 * default.
 *
 * <p>As with {@link DecodeFormat} we cannot directly set color spaces, we can only suggest to the
 * framework which one we want. Setting one of these values is not a guarantee that any returned
 * Bitmap will actually use the requested color space.
 */
public enum PreferredColorSpace {
  /** Prefers to decode images using {@link android.graphics.ColorSpace.Named#SRGB}. */
  SRGB,
  /** Prefers to decode images using {@link android.graphics.ColorSpace.Named#DISPLAY_P3}. */
  DISPLAY_P3,
}
