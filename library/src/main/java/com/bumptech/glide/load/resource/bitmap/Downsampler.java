package com.bumptech.glide.load.resource.bitmap;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import com.bumptech.glide.Logs;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.ByteArrayPool;
import com.bumptech.glide.util.ExceptionCatchingInputStream;
import com.bumptech.glide.util.MarkEnforcingInputStream;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Downsamples, decodes, and rotates images according to their exif orientation.
 */
public final class Downsampler implements ResourceDecoder<InputStream, Bitmap> {
  /**
   * A key for an {@link com.bumptech.glide.load.DecodeFormat} option that will be used in
   * conjunction with the image format to determine the {@link android.graphics.Bitmap.Config} to
   * provide to {@link android.graphics.BitmapFactory.Options#inPreferredConfig} when decoding
   * the image.
   */
  public static final String KEY_DECODE_FORMAT =
      "com.bumptech.glide.load.resource.bitmap.Downsampler.DecodeFormat";
  /**
   * A key for an {@link com.bumptech.glide.load.resource.bitmap.DownsampleStrategy} option that
   * will be used to calculate the sample size to use to downsample an image given the original
   * and target dimensions of the image.
   */
  public static final String KEY_DOWNSAMPLE_STRATEGY =
      "com.bumptech.glide.load.resource.bitmap.Downsampler.DownsampleStrategy";

  private static final Set<ImageHeaderParser.ImageType> TYPES_THAT_USE_POOL_PRE_KITKAT = EnumSet.of(
      ImageHeaderParser.ImageType.JPEG,
      ImageHeaderParser.ImageType.PNG_A,
      ImageHeaderParser.ImageType.PNG
  );
  private static final Queue<BitmapFactory.Options> OPTIONS_QUEUE = Util.createQueue(0);
  // 5MB. This is the max image header size we can handle, we preallocate a much smaller buffer
  // but will resize up to this amount if necessary.
  private static final int MARK_POSITION = 5 * 1024 * 1024;

  private final BitmapPool bitmapPool;

  public Downsampler(BitmapPool bitmapPool) {
    this.bitmapPool = Preconditions.checkNotNull(bitmapPool);
  }

  @Override
  public boolean handles(InputStream data) {
    // BitmapFactory should handle any valid image type.
    return true;
  }

  /**
   * Load the image for the given InputStream. If a recycled Bitmap whose dimensions exactly match
   * those of the image for the given InputStream is available, the operation is much less expensive
   * in terms of memory.
   *
   * <p> Note - this method will throw an exception of a Bitmap with dimensions not matching those
   * of the image for the given InputStream is provided. </p>
   *
   * @param is        An {@link InputStream} to the data for the image.
   * @param outWidth  The width the final image should be close to.
   * @param outHeight The height the final image should be close to.
   * @return A new bitmap containing the image from the given InputStream, or recycle if recycle is
   * not null.
   */
  @SuppressWarnings("resource")
  @Override
  public Resource<Bitmap> decode(InputStream is, int outWidth, int outHeight,
      Map<String, Object> options) throws IOException {
    ByteArrayPool byteArrayPool = ByteArrayPool.get();
    byte[] bytesForOptions = byteArrayPool.getBytes();
    byte[] bytesForStream = byteArrayPool.getBytes();
    BitmapFactory.Options bitmapFactoryOptions = getDefaultOptions();

    DecodeFormat decodeFormat = getDecodeFormat(options);
    DownsampleStrategy downsampleStrategy = getDownsampleStrategy(options);

    // Use to fix the mark limit to avoid allocating buffers that fit entire images.
    RecyclableBufferedInputStream bufferedStream =
        new RecyclableBufferedInputStream(is, bytesForStream);

    // Use to retrieve exceptions thrown while reading.
    // TODO(#126): when the framework no longer returns partially decoded Bitmaps or provides a
    // way to determine if a Bitmap is partially decoded, consider removing.
    ExceptionCatchingInputStream exceptionStream =
        ExceptionCatchingInputStream.obtain(bufferedStream);

    // Use to read data.
    // Ensures that we can always reset after reading an image header so that we can still
    // attempt to decode the full image even when the header decode fails and/or overflows our read
    // buffer. See #283.
    MarkEnforcingInputStream invalidatingStream = new MarkEnforcingInputStream(exceptionStream);

    try {
      Bitmap result = decodeFromWrappedStreams(exceptionStream, invalidatingStream, bufferedStream,
          bitmapFactoryOptions, bytesForOptions, downsampleStrategy, decodeFormat,
          outWidth, outHeight);
      return BitmapResource.obtain(result, bitmapPool);
    } finally {
      byteArrayPool.releaseBytes(bytesForOptions);
      byteArrayPool.releaseBytes(bytesForStream);
      exceptionStream.release();
      releaseOptions(bitmapFactoryOptions);
    }
  }

  private Bitmap decodeFromWrappedStreams(ExceptionCatchingInputStream exceptionStream,
      MarkEnforcingInputStream invalidatingStream, RecyclableBufferedInputStream bufferedStream,
      BitmapFactory.Options bitmapFactoryOptions, byte[] bytesForOptions,
      DownsampleStrategy downsampleStrategy, DecodeFormat decodeFormat, int outWidth, int outHeight)
      throws IOException {

    int orientation = getOrientation(exceptionStream);

    bitmapFactoryOptions.inTempStorage = bytesForOptions;

    final int[] inDimens =
        getDimensions(invalidatingStream, bufferedStream, bitmapFactoryOptions);
    final int inWidth = inDimens[0];
    final int inHeight = inDimens[1];

    final int degreesToRotate = TransformationUtils.getExifOrientationDegrees(orientation);
    final int sampleSize = getRoundedSampleSize(downsampleStrategy, degreesToRotate, inWidth,
        inHeight, outWidth, outHeight);

    final Bitmap downsampled =
        downsampleWithSize(invalidatingStream, bufferedStream, bitmapFactoryOptions, bitmapPool,
            inWidth, inHeight, sampleSize, decodeFormat);

    // BitmapFactory swallows exceptions during decodes and in some cases when inBitmap is non
    // null, may catch and log a stack trace but still return a non null bitmap. To avoid
    // displaying partially decoded bitmaps, we catch exceptions reading from the stream in our
    // ExceptionCatchingInputStream and throw them here.
    final Exception streamException = exceptionStream.getException();
    if (streamException != null) {
      if (Logs.isEnabled(Log.DEBUG)) {
        Logs.log(Log.DEBUG, "Exception thrown reading from stream in Downsampler",
            streamException);
      }
      if (downsampled != null && !bitmapPool.put(downsampled)) {
        downsampled.recycle();
      }
      return null;
    }

    Bitmap rotated = null;
    if (downsampled != null) {
      rotated = TransformationUtils.rotateImageExif(downsampled, bitmapPool, orientation);

      if (!downsampled.equals(rotated) && !bitmapPool.put(downsampled)) {
        downsampled.recycle();
      }
    }

    return rotated;
  }

  private int getRoundedSampleSize(DownsampleStrategy downsampleStrategy, int degreesToRotate,
      int inWidth, int inHeight, int outWidth, int outHeight) {
    int targetHeight = outHeight == Target.SIZE_ORIGINAL ? inHeight : outHeight;
    int targetWidth = outWidth == Target.SIZE_ORIGINAL ? inWidth : outWidth;

    final int exactSampleSize;
    if (degreesToRotate == 90 || degreesToRotate == 270) {
      // If we're rotating the image +-90 degrees, we need to downsample accordingly so the image
      // width is decreased to near our target's height and the image height is decreased to near
      // our target width.
      exactSampleSize =
          downsampleStrategy.getSampleSize(inHeight, inWidth, targetWidth, targetHeight);
    } else {
      exactSampleSize =
          downsampleStrategy.getSampleSize(inWidth, inHeight, targetWidth, targetHeight);
    }

    // BitmapFactory only accepts powers of 2, so it will round down to the nearest power of two
    // that is less than or equal to the sample size we provide. Because we need to estimate the
    // final image width and height to re-use Bitmaps, we mirror BitmapFactory's calculation here.
    // For bug, see issue #224. For algorithm see http://stackoverflow.com/a/17379704/800716.
    final int powerOfTwoSampleSize =
        exactSampleSize == 0 ? 0 : Integer.highestOneBit(exactSampleSize);

    // Although functionally equivalent to 0 for BitmapFactory, 1 is a safer default for our code
    // than 0.
    return Math.max(1, powerOfTwoSampleSize);
  }

  private static DownsampleStrategy getDownsampleStrategy(Map<String, Object> options) {
    return options.containsKey(KEY_DOWNSAMPLE_STRATEGY)
        ? (DownsampleStrategy) options.get(KEY_DOWNSAMPLE_STRATEGY) : DownsampleStrategy.DEFAULT;
  }

  private static DecodeFormat getDecodeFormat(Map<String, Object> options) {
    return options.containsKey(KEY_DECODE_FORMAT)
        ? (DecodeFormat) options.get(KEY_DECODE_FORMAT) : DecodeFormat.DEFAULT;
  }

  private static int getOrientation(ExceptionCatchingInputStream exceptionStream)
      throws IOException {
    exceptionStream.mark(MARK_POSITION);
    int orientation = 0;
    try {
      orientation = new ImageHeaderParser(exceptionStream).getOrientation();
    } catch (IOException e) {
      if (Logs.isEnabled(Log.DEBUG)) {
        Logs.log(Log.DEBUG, "Cannot determine the image orientation from header", e);
      }
    } finally {
      exceptionStream.reset();
    }
    return orientation;
  }

  private static Bitmap downsampleWithSize(MarkEnforcingInputStream is,
      RecyclableBufferedInputStream bufferedStream, BitmapFactory.Options options, BitmapPool pool,
      int inWidth, int inHeight, int sampleSize, DecodeFormat decodeFormat) throws IOException {
    // Prior to KitKat, the inBitmap size must exactly match the size of the bitmap we're decoding.
    Bitmap.Config config = getConfig(is, decodeFormat);
    options.inSampleSize = sampleSize;
    options.inPreferredConfig = config;
    if ((options.inSampleSize == 1 || Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT)
        && shouldUsePool(is)) {
      int targetWidth = (int) Math.ceil(inWidth / (double) sampleSize);
      int targetHeight = (int) Math.ceil(inHeight / (double) sampleSize);
      // BitmapFactory will clear out the Bitmap before writing to it, so getDirty is safe.
      setInBitmap(options, pool.getDirty(targetWidth, targetHeight, config));
    }
    return decodeStream(is, bufferedStream, options);
  }

  private static boolean shouldUsePool(InputStream is) throws IOException {
    // On KitKat+, any bitmap can be used to decode any other bitmap.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      return true;
    }

    is.mark(MARK_POSITION);
    try {
      final ImageHeaderParser.ImageType type = new ImageHeaderParser(is).getType();
      // We cannot reuse bitmaps when decoding images that are not PNG or JPG prior to KitKat.
      // See: https://groups.google.com/forum/#!msg/android-developers/Mp0MFVFi1Fo/e8ZQ9FGdWdEJ
      return TYPES_THAT_USE_POOL_PRE_KITKAT.contains(type);
    } catch (IOException e) {
      if (Logs.isEnabled(Log.DEBUG)) {
        Logs.log(Log.DEBUG, "Cannot determine the image type from header", e);
      }
    } finally {
      is.reset();
    }
    return false;
  }

  private static Bitmap.Config getConfig(InputStream is, DecodeFormat format) throws IOException {
    // Changing configs can cause skewing on 4.1, see issue #128.
    if (format == DecodeFormat.PREFER_ARGB_8888
        || Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
      return Bitmap.Config.ARGB_8888;
    }

    boolean hasAlpha = false;
    is.mark(MARK_POSITION);
    try {
      hasAlpha = new ImageHeaderParser(is).hasAlpha();
    } catch (IOException e) {
      if (Logs.isEnabled(Log.DEBUG)) {
        Logs.log(Log.DEBUG, "Cannot determine whether the image has alpha or not from header for"
            + " format " + format, e);
      }
    } finally {
      is.reset();
    }

    return hasAlpha ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
  }

  /**
   * A method for getting the dimensions of an image from the given InputStream.
   *
   * @param is      The InputStream representing the image.
   * @param options The options to pass to {@link BitmapFactory#decodeStream(java.io.InputStream,
   *                android.graphics.Rect, android.graphics.BitmapFactory.Options)}.
   * @return an array containing the dimensions of the image in the form {width, height}.
   */
  private static int[] getDimensions(MarkEnforcingInputStream is,
      RecyclableBufferedInputStream bufferedStream, BitmapFactory.Options options)
      throws IOException {
    options.inJustDecodeBounds = true;
    decodeStream(is, bufferedStream, options);
    options.inJustDecodeBounds = false;
    return new int[] { options.outWidth, options.outHeight };
  }

  private static Bitmap decodeStream(MarkEnforcingInputStream is,
      RecyclableBufferedInputStream bufferedStream, BitmapFactory.Options options)
      throws IOException {
    if (options.inJustDecodeBounds) {
      // This is large, but jpeg headers are not size bounded so we need something large enough
      // to minimize the possibility of not being able to fit enough of the header in the buffer to
      // get the image size so that we don't fail to load images. The BufferedInputStream will
      // create a new buffer of 2x the original size each time we use up the buffer space without
      // passing the mark so this is a maximum bound on the buffer size, not a default. Most of the
      // time we won't go past our pre-allocated 16kb.
      is.mark(MARK_POSITION);
    } else {
      // Once we've read the image header, we no longer need to allow the buffer to expand in
      // size. To avoid unnecessary allocations reading image data, we fix the mark limit so that it
      // is no larger than our current buffer size here. See issue #225.
      bufferedStream.fixMarkLimit();
    }

    final Bitmap result = BitmapFactory.decodeStream(is, null, options);

    if (options.inJustDecodeBounds) {
      is.reset();
    }

    return result;
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  private static void setInBitmap(BitmapFactory.Options options, Bitmap recycled) {
    if (Build.VERSION_CODES.HONEYCOMB <= Build.VERSION.SDK_INT) {
      options.inBitmap = recycled;
    }
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  private static synchronized BitmapFactory.Options getDefaultOptions() {
    BitmapFactory.Options decodeBitmapOptions;
    synchronized (OPTIONS_QUEUE) {
      decodeBitmapOptions = OPTIONS_QUEUE.poll();
    }
    if (decodeBitmapOptions == null) {
      decodeBitmapOptions = new BitmapFactory.Options();
      resetOptions(decodeBitmapOptions);
    }

    return decodeBitmapOptions;
  }

  private static void releaseOptions(BitmapFactory.Options decodeBitmapOptions) {
    resetOptions(decodeBitmapOptions);
    synchronized (OPTIONS_QUEUE) {
      OPTIONS_QUEUE.offer(decodeBitmapOptions);
    }
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  private static void resetOptions(BitmapFactory.Options decodeBitmapOptions) {
    decodeBitmapOptions.inTempStorage = null;
    decodeBitmapOptions.inDither = false;
    decodeBitmapOptions.inScaled = false;
    decodeBitmapOptions.inSampleSize = 1;
    decodeBitmapOptions.inPreferredConfig = null;
    decodeBitmapOptions.inJustDecodeBounds = false;
    decodeBitmapOptions.outWidth = 0;
    decodeBitmapOptions.outHeight = 0;
    decodeBitmapOptions.outMimeType = null;

    if (Build.VERSION_CODES.HONEYCOMB <= Build.VERSION.SDK_INT) {
      decodeBitmapOptions.inBitmap = null;
      decodeBitmapOptions.inMutable = true;
    }
  }
}
