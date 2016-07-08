package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.util.Log;
import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Util;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An {@link com.bumptech.glide.load.ResourceEncoder} that writes {@link android.graphics.Bitmap}s
 * to {@link java.io.OutputStream}s.
 *
 * <p> {@link android.graphics.Bitmap}s that return true from
 * {@link android.graphics.Bitmap#hasAlpha ()}} are written using
 * {@link android.graphics.Bitmap.CompressFormat#PNG}
 * to preserve alpha and all other bitmaps are written using
 * {@link android.graphics.Bitmap.CompressFormat#JPEG}. </p>
 *
 * @see android.graphics.Bitmap#compress(android.graphics.Bitmap.CompressFormat, int,
 * java.io.OutputStream)
 */
public class BitmapEncoder implements ResourceEncoder<Bitmap> {
  /**
   * An integer option between 0 and 100 that is used as the compression quality.
   *
   * <p> Defaults to 90. </p>
   */
  public static final Option<Integer> COMPRESSION_QUALITY = Option.memory(
      "com.bumptech.glide.load.resource.bitmap.BitmapEncoder.CompressionQuality", 90);

  /**
   * An {@link android.graphics.Bitmap.CompressFormat} option used as the format to encode
   * the {@link android.graphics.Bitmap}.
   *
   * <p> Defaults to {@link android.graphics.Bitmap.CompressFormat#JPEG} for images without alpha
   * and {@link android.graphics.Bitmap.CompressFormat#PNG} for images with alpha. </p>
   */
  public static final Option<Bitmap.CompressFormat> COMPRESSION_FORMAT = Option.memory(
      "com.bumptech.glide.load.resource.bitmap.BitmapEncoder.CompressionFormat");

  private static final String TAG = "BitmapEncoder";

  @Override
  public boolean encode(Resource<Bitmap> resource, File file, Options options) {
    final Bitmap bitmap = resource.get();

    long start = LogTime.getLogTime();
    Bitmap.CompressFormat format = getFormat(bitmap, options);
    int quality = options.get(COMPRESSION_QUALITY);

    boolean success = false;
    OutputStream os = null;
    try {
      os = new FileOutputStream(file);
      bitmap.compress(format, quality, os);
      os.close();
      success = true;
    } catch (IOException e) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Failed to encode Bitmap", e);
      }
    } finally {
      if (os != null) {
        try {
          os.close();
        } catch (IOException e) {
          // Do nothing.
        }
      }
    }

    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      Log.v(TAG, "Compressed with type: " + format + " of size " + Util.getBitmapByteSize(bitmap)
          + " in " + LogTime.getElapsedMillis(start));
    }
    return success;
  }

  private Bitmap.CompressFormat getFormat(Bitmap bitmap, Options options) {
    Bitmap.CompressFormat format = options.get(COMPRESSION_FORMAT);
    if (format != null) {
      return format;
    } else if (bitmap.hasAlpha()) {
      return Bitmap.CompressFormat.PNG;
    } else {
      return Bitmap.CompressFormat.JPEG;
    }
  }

  @Override
  public EncodeStrategy getEncodeStrategy(Options options) {
    return EncodeStrategy.TRANSFORMED;
  }
}
