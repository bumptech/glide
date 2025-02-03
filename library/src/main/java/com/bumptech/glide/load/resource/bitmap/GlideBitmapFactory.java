package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Gainmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.util.GlideSuppliers;
import com.bumptech.glide.util.GlideSuppliers.GlideSupplier;
import com.bumptech.glide.util.Preconditions;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper around {@link BitmapFactory} to work around known issues with {@link BitmapFactory}
 * across Android SDK levels.
 *
 * <p>In particular, this class works around these known issues:
 *
 * <ul>
 *   <li>Ultra HDR image single-channel gainmaps not being decoded on Android U when hardware
 *       bitmaps are enabled. This issue is further described in
 *       https://github.com/bumptech/glide/issues/5362.
 * </ul>
 *
 * <p>New usages of {@link BitmapFactory} APIs within Glide should be added here rather than called
 * directly.
 */
final class GlideBitmapFactory {

  private static final String TAG = "GlideBitmapFactory";

  private GlideBitmapFactory() {}

  /** Wrapper for {@link BitmapFactory#decodeStream}. */
  @Nullable
  public static Bitmap decodeStream(
      InputStream inputStream, BitmapFactory.Options options, ImageReader reader) {
    if (VERSION.SDK_INT == VERSION_CODES.UPSIDE_DOWN_CAKE
        && GainmapDecoderWorkaroundStateCalculator.needsGainmapDecodeWorkaround(options)
        && isLikelyToContainGainmap(reader)) {
      return safeAndExpensiveDecodeHardwareBitmapWithGainmap(inputStream, options);
    }
    return BitmapFactory.decodeStream(inputStream, /* outPadding= */ null, options);
  }

  /** Wrapper for {@link BitmapFactory#decodeByteArray}. */
  @Nullable
  public static Bitmap decodeByteArray(
      byte[] bytes, BitmapFactory.Options options, ImageReader reader) {
    if (VERSION.SDK_INT == VERSION_CODES.UPSIDE_DOWN_CAKE
        && GainmapDecoderWorkaroundStateCalculator.needsGainmapDecodeWorkaround(options)
        && isLikelyToContainGainmap(reader)) {
      return safeAndExpensiveDecodeHardwareBitmapWithGainmap(bytes, options);
    }
    return BitmapFactory.decodeByteArray(bytes, /* offset= */ 0, bytes.length, options);
  }

  /** Wrapper for {@link BitmapFactory#decodeFileDescriptor}. */
  @Nullable
  public static Bitmap decodeFileDescriptor(
      FileDescriptor fileDescriptor, BitmapFactory.Options options, ImageReader reader) {
    if (VERSION.SDK_INT == VERSION_CODES.UPSIDE_DOWN_CAKE
        && GainmapDecoderWorkaroundStateCalculator.needsGainmapDecodeWorkaround(options)
        && isLikelyToContainGainmap(reader)) {
      return safeAndExpensiveDecodeHardwareBitmapWithGainmap(fileDescriptor, options);
    }
    return BitmapFactory.decodeFileDescriptor(fileDescriptor, /* outPadding= */ null, options);
  }

  /**
   * Returns whether the image referenced by the {@link ImageReader} is likely to have a gainmap.
   *
   * <p>On Android devices, a JPEG with multi-picture format (MPF) metadata is very likely to
   * contain a gainmap, either it being an Ultra HDR JPEG or a ISO 21496-1 JPEG.
   */
  private static boolean isLikelyToContainGainmap(ImageReader imageReader) {
    try {
      boolean hasMpf = imageReader.hasJpegMpf();
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "isLikelyToContainGainmap=" + hasMpf);
      }
      return hasMpf;
    } catch (IOException e) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "isLikelyToContainGainmap failed", e);
      }
    }
    return false;
  }

  /**
   * Returns a decoded bitmap for the input stream, ensuring that any associated gainmap is decoded
   * without being silently dropped on Android U.
   *
   * <p>If the input stream does not reference an image with a gainmap, then this method simply
   * returns a hardware bitmap.
   *
   * <p>This method safely wraps BitmapFactory#decodeStream(InputStream, Rect, Options)} on Android
   * U.
   *
   * <p>This method performs an expensive workaround, using software bitmap decoding. It is
   * recommended to only use this check on images that have a reasonable chance of containing
   * gainmaps (e.g., they already contain JPEG multi-picture format metadata).
   *
   * @param inputStream for the bitmap to be decoded.
   * @param options to be applied in the {@link BitmapFactory#decodeStream} call.
   */
  @RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
  @Nullable
  private static Bitmap safeAndExpensiveDecodeHardwareBitmapWithGainmap(
      InputStream inputStream, Options options) {
    Preconditions.checkArgument(options.inPreferredConfig == Config.HARDWARE);
    Bitmap softwareBitmap = null;
    options.inPreferredConfig = Config.ARGB_8888;
    try {
      softwareBitmap = BitmapFactory.decodeStream(inputStream, /* outPadding= */ null, options);
      if (softwareBitmap == null) {
        return null;
      }
      return safeDecodeBitmapWithGainmap(softwareBitmap);
    } finally {
      if (softwareBitmap != null) {
        softwareBitmap.recycle();
      }
      options.inPreferredConfig = Config.HARDWARE;
    }
  }

  /**
   * Returns a decoded bitmap for the input byte array, ensuring that any associated gainmap is
   * decoded without being silently dropped on Android U.
   *
   * <p>If the input bytes do not reference an image with a gainmap, then this method simply returns
   * a hardware bitmap.
   *
   * <p>This method safely wraps BitmapFactory#decodeByteArray(byte[], int, int)} on Android U.
   *
   * @param bytes for the bitmap to be decoded.
   * @param options to be applied in the {@link BitmapFactory#decodeByteArray} call. This must be
   *     set to {@link Config#HARDWARE}.
   * @throws IllegalArgumentException if {@link Options#inPreferredConfig} is set to any state other
   *     than {@link Config#HARDWARE}.
   */
  @RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
  @Nullable
  private static Bitmap safeAndExpensiveDecodeHardwareBitmapWithGainmap(
      byte[] bytes, Options options) {
    Preconditions.checkArgument(options.inPreferredConfig == Config.HARDWARE);
    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    Bitmap softwareBitmap = null;
    try {
      softwareBitmap = BitmapFactory.decodeByteArray(bytes, /* offset= */ 0, bytes.length, options);
      if (softwareBitmap == null) {
        return null;
      }
      return GlideBitmapFactory.safeDecodeBitmapWithGainmap(softwareBitmap);
    } finally {
      if (softwareBitmap != null) {
        softwareBitmap.recycle();
      }
      options.inPreferredConfig = Config.HARDWARE;
    }
  }

  /**
   * Returns a decoded bitmap for the input file descriptor, ensuring that any associated gainmap is
   * decoded without being silently dropped on Android U.
   *
   * <p>If the input file descriptor does not reference an image with a gainmap, then this method
   * simply returns a hardware bitmap.
   *
   * <p>This method safely wraps {@link BitmapFactory#decodeFileDescriptor(FileDescriptor, Rect,
   * Options)} on Android U.
   *
   * @param fileDescriptor from which the bitmap will be decoded.
   * @param options to be applied in the {@link BitmapFactory#decodeFileDescriptor} call. This must
   *     be set to {@link Config#HARDWARE}.
   * @throws IllegalArgumentException if {@link Options#inPreferredConfig} is set to any state other
   *     than {@link Config#HARDWARE}.
   */
  @RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
  @Nullable
  private static Bitmap safeAndExpensiveDecodeHardwareBitmapWithGainmap(
      FileDescriptor fileDescriptor, Options options) {
    Preconditions.checkArgument(options.inPreferredConfig == Config.HARDWARE);
    Bitmap softwareBitmap = null;
    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    try {
      softwareBitmap =
          BitmapFactory.decodeFileDescriptor(fileDescriptor, /* outPadding= */ null, options);
      if (softwareBitmap == null) {
        return null;
      }
      return GlideBitmapFactory.safeDecodeBitmapWithGainmap(softwareBitmap);
    } finally {
      if (softwareBitmap != null) {
        softwareBitmap.recycle();
      }
      options.inPreferredConfig = Config.HARDWARE;
    }
  }

  /**
   * Returns a decoded bitmap for the input software bitmap, ensuring that any associated gainmap is
   * decoded without errors on Android U if it is a valid gainmap.
   *
   * @param softwareBitmap The bitmap to be decoded. Must not be a hardware bitmap. The caller of
   *     this method is responsible for recycling this bitmap.
   * @throws IllegalArgumentException if {@link Options#inPreferredConfig} is set to any state other
   *     than {@link Config#HARDWARE}.
   */
  @RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
  @Nullable
  private static Bitmap safeDecodeBitmapWithGainmap(Bitmap softwareBitmap) {
    Gainmap gainmap = softwareBitmap.getGainmap();
    if (gainmap != null) {
      Bitmap gainmapContents = gainmap.getGainmapContents();
      if (gainmapContents.getConfig() == Config.ALPHA_8) {
        softwareBitmap.setGainmap(
            GainmapCopier.convertSingleChannelGainmapToTripleChannelGainmap(gainmap));
      }
    }
    return softwareBitmap.copy(Config.HARDWARE, /* isMutable= */ false);
  }

  /** Utils to copy gainmaps. */
  @RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
  private static final class GainmapCopier {

    /** Transforms a bitmap so that the output alpha is opaque. */
    private static final ColorMatrixColorFilter OPAQUE_FILTER =
        new ColorMatrixColorFilter(
            new float[] {
                0f, 0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f, 0f,
                0f, 0f, 0f, 0f, 255f
            });

    private GainmapCopier() {}

    /**
     * Converts single channel gainmap to triple channel, where a single channel gainmap is defined
     * as a gainmap with a bitmap config of {@link Config#ALPHA_8}.
     *
     * <p>If the input gainmap is not single channel or the copy operation fails, then this method
     * will just return the original gainmap.
     */
    public static Gainmap convertSingleChannelGainmapToTripleChannelGainmap(Gainmap gainmap) {
      Bitmap gainmapContents = gainmap.getGainmapContents();
      if (gainmapContents.getConfig() != Config.ALPHA_8) {
        return gainmap;
      }
      Bitmap newContents = copyAlpha8ToOpaqueArgb888(gainmapContents);
      Gainmap newGainmap = new Gainmap(newContents);
      float[] tempFloatArray = gainmap.getRatioMin();
      newGainmap.setRatioMin(tempFloatArray[0], tempFloatArray[1], tempFloatArray[2]);
      tempFloatArray = gainmap.getRatioMax();
      newGainmap.setRatioMax(tempFloatArray[0], tempFloatArray[1], tempFloatArray[2]);
      tempFloatArray = gainmap.getGamma();
      newGainmap.setGamma(tempFloatArray[0], tempFloatArray[1], tempFloatArray[2]);
      tempFloatArray = gainmap.getEpsilonSdr();
      newGainmap.setEpsilonSdr(tempFloatArray[0], tempFloatArray[1], tempFloatArray[2]);
      tempFloatArray = gainmap.getEpsilonHdr();
      newGainmap.setEpsilonHdr(tempFloatArray[0], tempFloatArray[1], tempFloatArray[2]);
      newGainmap.setDisplayRatioForFullHdr(gainmap.getDisplayRatioForFullHdr());
      newGainmap.setMinDisplayRatioForHdrTransition(gainmap.getMinDisplayRatioForHdrTransition());
      return newGainmap;
    }

    /**
     * Converts an {@link Config#ALPHA_8} bitmap to a {@link Config#ARGB_8888} bitmap with the alpha
     * channel set to unity so that the output bitmap is opaque.
     *
     * @throws IllegalArgumentException if called with a bitmap with a config that is not {@link
     *     Config#ALPHA_8}
     */
    private static Bitmap copyAlpha8ToOpaqueArgb888(Bitmap bitmap) {
      Preconditions.checkArgument(bitmap.getConfig() == Config.ALPHA_8);
      // We have to use a canvas operation with an opaque alpha filter to draw the gainmap. We can't
      // use bitmap.copy(Config.ARGB_8888, /* isMutable= */ false) because copying from A8 to RBGA
      // will result in zero-valued RGB values.
      Bitmap newContents =
          Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
      Canvas canvas = new Canvas(newContents);
      Paint paint = new Paint();
      paint.setColorFilter(OPAQUE_FILTER);
      canvas.drawBitmap(bitmap, /* left= */ 0f, /* top= */ 0f, paint);
      canvas.setBitmap(null);
      return newContents;
    }
  }

  /**
   * Determines if a gainmap decoding workaround is required to mitigate an Android U bug with
   * decoding bitmaps with gainmaps. When the following conditions are present, Android U will not
   * be able to decode a gainmap, with hardware bitmap operation failing for the gainmap:
   *
   * <ul>
   *   <li>The HWUI is configured to use skiagl.
   *   <li>The gainmap is single channel.
   *   <li>The bitmap owning the bitmap is a hardware bitmap.
   * </ul>
   *
   * <p>Callers should use this class to determine whether to apply a workaround, e.g., modifying
   * the gainmap to be triple channel and software decode it.
   */
  public static final class GainmapDecoderWorkaroundStateCalculator {
    private static final String TAG = "GainmapWorkaroundCalc";

    /** Meomizes result of test to see if the device is susceptible to the gainmap decoding bug. */
    private static final GlideSupplier<Boolean> REQUIRES_GAIN_MAP_FIX =
        GlideSuppliers.memorize(() -> calculateNeedsGainmapDecodeWorkaround());

    private GainmapDecoderWorkaroundStateCalculator() {}

    /**
     * Returns true if a gainmap decoding workaround is required to mitigate an Android U bug. This
     * method tests for the presence of the bug, which only affects hardware bitmaps, and caches the
     * result in memory.
     *
     * <p>This method is thread-safe.
     *
     * @param options which will be used to decode the gainmap.
     */
    private static boolean needsGainmapDecodeWorkaround(Options options) {
      if (VERSION.SDK_INT != VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return false;
      }
      if (options.inPreferredConfig != Config.HARDWARE) {
        return false;
      }
      return REQUIRES_GAIN_MAP_FIX.get();
    }

    private static boolean calculateNeedsGainmapDecodeWorkaround() {
      if (VERSION.SDK_INT != VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return false;
      }
      // Create a 1x1 single channel, A8 bitmap and attempt to copy to a hardware bitmap. If the
      // copy operation fails, then the device requires a workaround to decode hardware
      // gainmaps.
      Bitmap a8Source = Bitmap.createBitmap(/* width= */ 1, /* height= */ 1, Config.ALPHA_8);
      Bitmap a8HardwareBitmap = a8Source.copy(Config.HARDWARE, /* isMutable= */ false);
      a8Source.recycle();
      boolean needsGainmapDecodeWorkaround = a8HardwareBitmap == null;
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "calculateNeedsGainmapDecodeWorkaround=" + needsGainmapDecodeWorkaround);
      }
      if (a8HardwareBitmap != null) {
        a8HardwareBitmap.recycle();
      }
      return needsGainmapDecodeWorkaround;
    }
  }
}
