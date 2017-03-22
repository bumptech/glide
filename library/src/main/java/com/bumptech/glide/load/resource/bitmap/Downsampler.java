package com.bumptech.glide.load.resource.bitmap;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy.SampleSizeRounding;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Downsamples, decodes, and rotates images according to their exif orientation.
 */
public final class Downsampler {
  private static final String TAG = "Downsampler";
  /**
   * Indicates the {@link com.bumptech.glide.load.DecodeFormat} that will be used in conjunction
   * with the image format to determine the {@link android.graphics.Bitmap.Config} to provide to
   * {@link android.graphics.BitmapFactory.Options#inPreferredConfig} when decoding the image.
   */
  public static final Option<DecodeFormat> DECODE_FORMAT = Option.memory(
      "com.bumptech.glide.load.resource.bitmap.Downsampler.DecodeFormat", DecodeFormat.DEFAULT);
  /**
   * Indicates the {@link com.bumptech.glide.load.resource.bitmap.DownsampleStrategy} option that
   * will be used to calculate the sample size to use to downsample an image given the original
   * and target dimensions of the image.
   */
  public static final Option<DownsampleStrategy> DOWNSAMPLE_STRATEGY =
      Option.memory("com.bumptech.glide.load.resource.bitmap.Downsampler.DownsampleStrategy",
          DownsampleStrategy.AT_LEAST);

  /**
   * Ensure that the size of the bitmap is fixed to the requested width and height of the
   * resource from the caller.  The final resource dimensions may differ from the requested
   * width and height, and thus setting this to true may result in the bitmap size differing
   * from the resource dimensions.
   *
   * This can be used as a performance optimization for KitKat and above by fixing the size of the
   * bitmap for a collection of requested resources so that the bitmap pool will not need to
   * allocate new bitmaps for images of different sizes.
   */
  public static final Option<Boolean> FIX_BITMAP_SIZE_TO_REQUESTED_DIMENSIONS =
      Option.memory("com.bumptech.glide.load.resource.bitmap.Downsampler.FixBitmapSize", false);

  private static final String WBMP_MIME_TYPE = "image/vnd.wap.wbmp";
  private static final String ICO_MIME_TYPE = "image/x-ico";
  private static final Set<String> NO_DOWNSAMPLE_PRE_N_MIME_TYPES =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(
                  WBMP_MIME_TYPE,
                  ICO_MIME_TYPE
              )
          )
      );
  private static final DecodeCallbacks EMPTY_CALLBACKS = new DecodeCallbacks() {
    @Override
    public void onObtainBounds() {
      // Do nothing.
    }

    @Override
    public void onDecodeComplete(BitmapPool bitmapPool, Bitmap downsampled) throws IOException {
      // Do nothing.
    }
  };
  private static final Set<ImageHeaderParser.ImageType> TYPES_THAT_USE_POOL_PRE_KITKAT =
      Collections.unmodifiableSet(
          EnumSet.of(
              ImageHeaderParser.ImageType.JPEG,
              ImageHeaderParser.ImageType.PNG_A,
              ImageHeaderParser.ImageType.PNG
          )
      );
  private static final Queue<BitmapFactory.Options> OPTIONS_QUEUE = Util.createQueue(0);
  // 5MB. This is the max image header size we can handle, we preallocate a much smaller buffer
  // but will resize up to this amount if necessary.
  private static final int MARK_POSITION = 5 * 1024 * 1024;

  private final BitmapPool bitmapPool;
  private final DisplayMetrics displayMetrics;
  private final ArrayPool byteArrayPool;
  private final List<ImageHeaderParser> parsers;

  public Downsampler(List<ImageHeaderParser> parsers, DisplayMetrics displayMetrics,
      BitmapPool bitmapPool, ArrayPool byteArrayPool) {
    this.parsers = parsers;
    this.displayMetrics = Preconditions.checkNotNull(displayMetrics);
    this.bitmapPool = Preconditions.checkNotNull(bitmapPool);
    this.byteArrayPool = Preconditions.checkNotNull(byteArrayPool);
  }

  public boolean handles(InputStream is) {
    // We expect Downsampler to handle any available type Android supports.
    return true;
  }

  public boolean handles(ByteBuffer byteBuffer) {
    // We expect downsampler to handle any available type Android supports.
    return true;
  }

  /**
   * Returns a Bitmap decoded from the given {@link InputStream} that is rotated to match any EXIF
   * data present in the stream and that is downsampled according to the given dimensions and any
   * provided  {@link com.bumptech.glide.load.resource.bitmap.DownsampleStrategy} option.
   *
   * @see #decode(InputStream, int, int, Options, DecodeCallbacks)
   */
  public Resource<Bitmap> decode(InputStream is, int outWidth, int outHeight,
      Options options) throws IOException {
    return decode(is, outWidth, outHeight, options, EMPTY_CALLBACKS);
  }

  /**
   * Returns a Bitmap decoded from the given {@link InputStream} that is rotated to match any EXIF
   * data present in the stream and that is downsampled according to the given dimensions and any
   * provided  {@link com.bumptech.glide.load.resource.bitmap.DownsampleStrategy} option.
   *
   * <p> If a Bitmap is present in the
   * {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} whose dimensions exactly match
   * those of the image for the given InputStream is available, the operation is much less expensive
   * in terms of memory. </p>
   *
   * <p> The provided {@link java.io.InputStream} must return <code>true</code> from
   * {@link java.io.InputStream#markSupported()} and is expected to support a reasonably large
   * mark limit to accommodate reading large image headers (~5MB). </p>
   *
   * @param is        An {@link InputStream} to the data for the image.
   * @param requestedWidth  The width the final image should be close to.
   * @param requestedHeight The height the final image should be close to.
   * @param options   A set of options that may contain one or more supported options that influence
   *                  how a Bitmap will be decoded from the given stream.
   * @param callbacks A set of callbacks allowing callers to optionally respond to various
   *                  significant events during the decode process.
   * @return A new bitmap containing the image from the given InputStream, or recycle if recycle is
   * not null.
   */
  @SuppressWarnings("resource")
  public Resource<Bitmap> decode(InputStream is, int requestedWidth, int requestedHeight,
      Options options, DecodeCallbacks callbacks) throws IOException {
    Preconditions.checkArgument(is.markSupported(), "You must provide an InputStream that supports"
        + " mark()");

    byte[] bytesForOptions = byteArrayPool.get(ArrayPool.STANDARD_BUFFER_SIZE_BYTES, byte[].class);
    BitmapFactory.Options bitmapFactoryOptions = getDefaultOptions();
    bitmapFactoryOptions.inTempStorage = bytesForOptions;

    DecodeFormat decodeFormat = options.get(DECODE_FORMAT);
    DownsampleStrategy downsampleStrategy = options.get(DOWNSAMPLE_STRATEGY);
    boolean fixBitmapToRequestedDimensions = options.get(FIX_BITMAP_SIZE_TO_REQUESTED_DIMENSIONS);

    try {
      Bitmap result = decodeFromWrappedStreams(is, bitmapFactoryOptions,
          downsampleStrategy, decodeFormat, requestedWidth, requestedHeight,
          fixBitmapToRequestedDimensions, callbacks);
      return BitmapResource.obtain(result, bitmapPool);
    } finally {
      releaseOptions(bitmapFactoryOptions);
      byteArrayPool.put(bytesForOptions, byte[].class);
    }
  }

  private Bitmap decodeFromWrappedStreams(InputStream is,
      BitmapFactory.Options options, DownsampleStrategy downsampleStrategy,
      DecodeFormat decodeFormat, int requestedWidth, int requestedHeight,
      boolean fixBitmapToRequestedDimensions, DecodeCallbacks callbacks) throws IOException {

    int[] sourceDimensions = getDimensions(is, options, callbacks);
    int sourceWidth = sourceDimensions[0];
    int sourceHeight = sourceDimensions[1];
    String sourceMimeType = options.outMimeType;

    int orientation = ImageHeaderParserUtils.getOrientation(parsers, is, byteArrayPool);
    int degreesToRotate = TransformationUtils.getExifOrientationDegrees(orientation);

    options.inPreferredConfig = getConfig(is, decodeFormat);
    if (options.inPreferredConfig != Bitmap.Config.ARGB_8888) {
      options.inDither = true;
    }

    int targetWidth = requestedWidth == Target.SIZE_ORIGINAL ? sourceWidth : requestedWidth;
    int targetHeight = requestedHeight == Target.SIZE_ORIGINAL ? sourceHeight : requestedHeight;

    calculateScaling(downsampleStrategy, degreesToRotate, sourceWidth, sourceHeight, targetWidth,
        targetHeight, options);

    boolean isKitKatOrGreater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    // Prior to KitKat, the inBitmap size must exactly match the size of the bitmap we're decoding.
    if ((options.inSampleSize == 1 || isKitKatOrGreater)
        && shouldUsePool(is)) {
      int expectedWidth;
      int expectedHeight;
      if (fixBitmapToRequestedDimensions && isKitKatOrGreater) {
        expectedWidth = targetWidth;
        expectedHeight = targetHeight;
      } else {
        float densityMultiplier = isScaling(options)
            ? (float) options.inTargetDensity / options.inDensity : 1f;
        int sampleSize = options.inSampleSize;
        int downsampledWidth = (int) Math.ceil(sourceWidth / (float) sampleSize);
        int downsampledHeight = (int) Math.ceil(sourceHeight / (float) sampleSize);
        expectedWidth = Math.round(downsampledWidth * densityMultiplier);
        expectedHeight = Math.round(downsampledHeight * densityMultiplier);

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
          Log.v(TAG, "Calculated target [" + expectedWidth + "x" + expectedHeight + "] for source"
              + " [" + sourceWidth + "x" + sourceHeight + "]"
              + ", sampleSize: " + sampleSize
              + ", targetDensity: " + options.inTargetDensity
              + ", density: " + options.inDensity
              + ", density multiplier: " + densityMultiplier);
        }
      }
      // If this isn't an image, or BitmapFactory was unable to parse the size, width and height
      // will be -1 here.
      if (expectedWidth > 0 && expectedHeight > 0) {
        setInBitmap(options, bitmapPool, expectedWidth, expectedHeight);
      }
    }
    Bitmap downsampled = decodeStream(is, options, callbacks);
    callbacks.onDecodeComplete(bitmapPool, downsampled);

    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      logDecode(sourceWidth, sourceHeight, sourceMimeType, options, downsampled,
          requestedWidth, requestedHeight);
    }

    Bitmap rotated = null;
    if (downsampled != null) {
      // If we scaled, the Bitmap density will be our inTargetDensity. Here we correct it back to
      // the expected density dpi.
      downsampled.setDensity(displayMetrics.densityDpi);

      rotated = TransformationUtils.rotateImageExif(bitmapPool, downsampled, orientation);
      if (!downsampled.equals(rotated)) {
        bitmapPool.put(downsampled);
      }
    }

    return rotated;
  }

  // Visible for testing.
  static void calculateScaling(DownsampleStrategy downsampleStrategy, int degreesToRotate,
      int sourceWidth, int sourceHeight, int targetWidth, int targetHeight,
      BitmapFactory.Options options) {
    // We can't downsample source content if we can't determine its dimensions.
    if (sourceWidth <= 0 || sourceHeight <= 0) {
      return;
    }

    final float exactScaleFactor;
    if (degreesToRotate == 90 || degreesToRotate == 270) {
      // If we're rotating the image +-90 degrees, we need to downsample accordingly so the image
      // width is decreased to near our target's height and the image height is decreased to near
      // our target width.
      //noinspection SuspiciousNameCombination
      exactScaleFactor = downsampleStrategy.getScaleFactor(sourceHeight, sourceWidth,
          targetWidth, targetHeight);
    } else {
      exactScaleFactor =
          downsampleStrategy.getScaleFactor(sourceWidth, sourceHeight, targetWidth, targetHeight);
    }

    if (exactScaleFactor <= 0f) {
      throw new IllegalArgumentException("Cannot scale with factor: " + exactScaleFactor
          + " from: " + downsampleStrategy);
    }
    SampleSizeRounding rounding = downsampleStrategy.getSampleSizeRounding(sourceWidth,
        sourceHeight, targetWidth, targetHeight);
    if (rounding == null) {
      throw new IllegalArgumentException("Cannot round with null rounding");
    }

    int outWidth = (int) (exactScaleFactor * sourceWidth + 0.5f);
    int outHeight = (int) (exactScaleFactor * sourceHeight + 0.5f);

    int widthScaleFactor = sourceWidth / outWidth;
    int heightScaleFactor = sourceHeight / outHeight;

    int scaleFactor = rounding == SampleSizeRounding.MEMORY
        ? Math.max(widthScaleFactor, heightScaleFactor)
        : Math.min(widthScaleFactor, heightScaleFactor);

    int powerOfTwoSampleSize;
    // BitmapFactory does not support downsampling wbmp files on platforms <= M. See b/27305903.
    if (Build.VERSION.SDK_INT <= 23
        && NO_DOWNSAMPLE_PRE_N_MIME_TYPES.contains(options.outMimeType)) {
      powerOfTwoSampleSize = 1;
    } else {
      powerOfTwoSampleSize = Math.max(1, Integer.highestOneBit(scaleFactor));
      if (rounding == SampleSizeRounding.MEMORY
          && powerOfTwoSampleSize < (1.f / exactScaleFactor)) {
        powerOfTwoSampleSize = powerOfTwoSampleSize << 1;
      }
    }

    float adjustedScaleFactor = powerOfTwoSampleSize * exactScaleFactor;

    options.inSampleSize = powerOfTwoSampleSize;
    // Density scaling is only supported if inBitmap is null prior to KitKat. Avoid setting
    // densities here so we calculate the final Bitmap size correctly.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      options.inTargetDensity = (int) (1000 * adjustedScaleFactor + 0.5f);
      options.inDensity = 1000;
    }
    if (isScaling(options)) {
      options.inScaled = true;
    } else {
      options.inDensity = options.inTargetDensity = 0;
    }

    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      Log.v(TAG, "Calculate scaling"
          + ", source: [" + sourceWidth + "x" + sourceHeight + "]"
          + ", target: [" + targetWidth + "x" + targetHeight + "]"
          + ", exact scale factor: " + exactScaleFactor
          + ", power of 2 sample size: " + powerOfTwoSampleSize
          + ", adjusted scale factor: " + adjustedScaleFactor
          + ", target density: " + options.inTargetDensity
          + ", density: " + options.inDensity);
    }
  }

  private boolean shouldUsePool(InputStream is) throws IOException {
    // On KitKat+, any bitmap (of a given config) can be used to decode any other bitmap
    // (with the same config).
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      return true;
    }

    try {
      ImageHeaderParser.ImageType type = ImageHeaderParserUtils.getType(parsers, is, byteArrayPool);
      // We cannot reuse bitmaps when decoding images that are not PNG or JPG prior to KitKat.
      // See: https://groups.google.com/forum/#!msg/android-developers/Mp0MFVFi1Fo/e8ZQ9FGdWdEJ
      return TYPES_THAT_USE_POOL_PRE_KITKAT.contains(type);
    } catch (IOException e) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Cannot determine the image type from header", e);
      }
    }
    return false;
  }

  private Bitmap.Config getConfig(InputStream is, DecodeFormat format) throws IOException {
    // Changing configs can cause skewing on 4.1, see issue #128.
    if (format == DecodeFormat.PREFER_ARGB_8888
        || Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
      return Bitmap.Config.ARGB_8888;
    }

    boolean hasAlpha = false;
    try {
      hasAlpha = ImageHeaderParserUtils.getType(parsers, is, byteArrayPool).hasAlpha();
    } catch (IOException e) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Cannot determine whether the image has alpha or not from header"
            + ", format " + format, e);
      }
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
  private static int[] getDimensions(InputStream is, BitmapFactory.Options options,
      DecodeCallbacks decodeCallbacks) throws IOException {
    options.inJustDecodeBounds = true;
    decodeStream(is, options, decodeCallbacks);
    options.inJustDecodeBounds = false;
    return new int[] { options.outWidth, options.outHeight };
  }

  private static Bitmap decodeStream(InputStream is, BitmapFactory.Options options,
      DecodeCallbacks callbacks) throws IOException {
    if (options.inJustDecodeBounds) {
      is.mark(MARK_POSITION);
    } else {
      // Once we've read the image header, we no longer need to allow the buffer to expand in
      // size. To avoid unnecessary allocations reading image data, we fix the mark limit so that it
      // is no larger than our current buffer size here. We need to do so immediately before
      // decoding the full image to avoid having our mark limit overridden by other calls to
      // markand reset. See issue #225.
      callbacks.onObtainBounds();
    }
    // BitmapFactory.Options out* variables are reset by most calls to decodeStream, successful or
    // otherwise, so capture here in case we log below.
    int sourceWidth = options.outWidth;
    int sourceHeight = options.outHeight;
    String outMimeType = options.outMimeType;
    final Bitmap result;
    TransformationUtils.getBitmapDrawableLock().lock();
    try {
      result = BitmapFactory.decodeStream(is, null, options);
    } catch (IllegalArgumentException e) {
      throw newIoExceptionForInBitmapAssertion(e, sourceWidth, sourceHeight, outMimeType, options);
    } finally {
      TransformationUtils.getBitmapDrawableLock().unlock();
    }

    if (options.inJustDecodeBounds) {
      is.reset();

    }
    return result;
  }

  private static boolean isScaling(BitmapFactory.Options options) {
    return options.inTargetDensity > 0 && options.inDensity > 0
        && options.inTargetDensity != options.inDensity;
  }

  private static void logDecode(int sourceWidth, int sourceHeight, String outMimeType,
      BitmapFactory.Options options, Bitmap result, int requestedWidth, int requestedHeight) {
    Log.v(TAG, "Decoded " + getBitmapString(result)
        + " from [" + sourceWidth + "x" + sourceHeight + "] " + outMimeType
        + " with inBitmap " + getInBitmapString(options)
        + " for [" + requestedWidth + "x" + requestedHeight + "]"
        + ", sample size: " + options.inSampleSize
        + ", density: " + options.inDensity
        + ", target density: " + options.inTargetDensity
        + ", thread: " + Thread.currentThread().getName());
  }

  private static String getInBitmapString(BitmapFactory.Options options) {
    return getBitmapString(options.inBitmap);
  }

  @Nullable
  @TargetApi(Build.VERSION_CODES.KITKAT)
  private static String getBitmapString(Bitmap bitmap) {
    if (bitmap == null) {
      return null;
    }

    String sizeString = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
        ? " (" + bitmap.getAllocationByteCount() + ")" : "";
    return  "[" + bitmap.getWidth() + "x" + bitmap.getHeight() + "] " + bitmap.getConfig()
        + sizeString;
  }

  // BitmapFactory throws an IllegalArgumentException if any error occurs attempting to decode a
  // file when inBitmap is non-null, including those caused by partial or corrupt data. We still log
  // the error because the IllegalArgumentException is supposed to catch errors reusing Bitmaps, so
  // want some useful log output. In most cases this can be safely treated as a normal IOException.
  private static IOException newIoExceptionForInBitmapAssertion(IllegalArgumentException e,
      int outWidth, int outHeight, String outMimeType, BitmapFactory.Options options) {
    return new IOException("Exception decoding bitmap"
          + ", outWidth: " + outWidth
          + ", outHeight: " + outHeight
          + ", outMimeType: " + outMimeType
          + ", inBitmap: " + getInBitmapString(options), e);
  }

  private static void setInBitmap(BitmapFactory.Options options, BitmapPool bitmapPool, int width,
      int height) {
    // BitmapFactory will clear out the Bitmap before writing to it, so getDirty is safe.
    options.inBitmap = bitmapPool.getDirty(width, height, options.inPreferredConfig);
  }

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

  private static void resetOptions(BitmapFactory.Options decodeBitmapOptions) {
    decodeBitmapOptions.inTempStorage = null;
    decodeBitmapOptions.inDither = false;
    decodeBitmapOptions.inScaled = false;
    decodeBitmapOptions.inSampleSize = 1;
    decodeBitmapOptions.inPreferredConfig = null;
    decodeBitmapOptions.inJustDecodeBounds = false;
    decodeBitmapOptions.inDensity = 0;
    decodeBitmapOptions.inTargetDensity = 0;
    decodeBitmapOptions.outWidth = 0;
    decodeBitmapOptions.outHeight = 0;
    decodeBitmapOptions.outMimeType = null;
    decodeBitmapOptions.inBitmap = null;
    decodeBitmapOptions.inMutable = true;
  }

  /**
   * Callbacks for key points during decodes.
   */
  public interface DecodeCallbacks {
    void onObtainBounds();
    void onDecodeComplete(BitmapPool bitmapPool, Bitmap downsampled) throws IOException;
  }
}
