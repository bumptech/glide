package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.util.Log;

import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;

import java.io.OutputStream;
import java.util.Map;

/**
 * An {@link com.bumptech.glide.load.ResourceEncoder} that writes {@link android.graphics.Bitmap}s
 * to {@link java.io.OutputStream}s.
 *
 * <p> {@link android.graphics.Bitmap}s that return true from {@link
 * android.graphics.Bitmap#hasAlpha ()}} are written using {@link android.graphics.Bitmap
 * .CompressFormat#PNG}
 * to preserve alpha and all other bitmaps are written using {@link android.graphics.Bitmap
 * .CompressFormat#JPEG}.
 * </p>
 *
 * @see android.graphics.Bitmap#compress(android.graphics.Bitmap.CompressFormat, int,
 * java.io.OutputStream)
 */
public class BitmapEncoder implements ResourceEncoder<Bitmap> {
  /**
   * A key for an integer option between 0 and 100 that is used as the compression quality.
   *
   * <p> Defaults to 90. </p>
   */
  public static final String KEY_COMPRESSION_QUALITY =
      "com.bumptech.glide.load.resource.bitmap.BitmapEncoder.CompressionQuality";
  /**
   * A key for an {@link android.graphics.Bitmap.CompressFormat} option used as the format to encode
   * the {@link android.graphics.Bitmap}.
   *
   * <p> Defaults to {@link android.graphics.Bitmap.CompressFormat#JPEG} for images without alpha
   * and {@link android.graphics.Bitmap.CompressFormat#PNG} for images with alpha. </p>
   */
  public static final String KEY_COMPRESSION_FORMAT =
      "com.bumptech.glide.load.resource.bitmap.BitmapEncoder.CompressionFormat";

  private static final String TAG = "BitmapEncoder";
  private static final int DEFAULT_COMPRESSION_QUALITY = 90;

  @Override
  public boolean encode(Resource<Bitmap> resource, OutputStream os, Map<String, Object> options) {
    final Bitmap bitmap = resource.get();

    long start = LogTime.getLogTime();
    Bitmap.CompressFormat format = getFormat(bitmap, options);
    int quality = getQuality(options);
    bitmap.compress(format, quality, os);

    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      Log.v(TAG,
          "Compressed with type: " + format + " of size " + Util.getBitmapByteSize(bitmap) + " in "
              + LogTime.getElapsedMillis(start));
    }
    return true;
  }

  private int getQuality(Map<String, Object> options) {
    Integer quality = (Integer) options.get(KEY_COMPRESSION_QUALITY);
    Preconditions.checkArgument(quality == null || quality >= 0 && quality <= 100,
        "quality must be between 0 and 100");
    return quality == null ? DEFAULT_COMPRESSION_QUALITY : quality;
  }

  private Bitmap.CompressFormat getFormat(Bitmap bitmap, Map<String, Object> options) {
    Bitmap.CompressFormat format = (Bitmap.CompressFormat) options.get(KEY_COMPRESSION_FORMAT);
    if (format != null) {
      return format;
    } else if (bitmap.hasAlpha()) {
      return Bitmap.CompressFormat.PNG;
    } else {
      return Bitmap.CompressFormat.JPEG;
    }
  }

  @Override
  public EncodeStrategy getEncodeStrategy(Map<String, Object> options) {
    return EncodeStrategy.TRANSFORMED;
  }
}
