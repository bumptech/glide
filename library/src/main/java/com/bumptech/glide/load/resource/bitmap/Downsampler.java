package com.bumptech.glide.load.resource.bitmap;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParser.ImageType;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.PreferredColorSpace;
import com.bumptech.glide.load.data.ParcelFileDescriptorRewinder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy.SampleSizeRounding;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.LogTime;
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
 * Downsamples, decodes, and rotates images according to their exif orientation using {@link
 * BitmapFactory}.
 */
public final class Downsampler {
  static final String TAG = "Downsampler";

  /**
   * Indicates the {@link com.bumptech.glide.load.DecodeFormat} that will be used in conjunction
   * with the image format to determine the {@link android.graphics.Bitmap.Config} to provide to
   * {@link android.graphics.BitmapFactory.Options#inPreferredConfig} when decoding the image.
   */
  public static final Option<DecodeFormat> DECODE_FORMAT =
      Option.memory(
          "com.bumptech.glide.load.resource.bitmap.Downsampler.DecodeFormat", DecodeFormat.DEFAULT);

  /**
   * Sets the {@link PreferredColorSpace} that will be used along with the version of Android and
   * color space of the requested image to determine the final color space used to decode the image.
   *
   * <p>Refer to {@link PreferredColorSpace} for details on how this option works and its various
   * limitations.
   */
  public static final Option<PreferredColorSpace> PREFERRED_COLOR_SPACE =
      Option.memory(
          "com.bumptech.glide.load.resource.bitmap.Downsampler.PreferredColorSpace",
          PreferredColorSpace.SRGB);
  /**
   * Indicates the {@link com.bumptech.glide.load.resource.bitmap.DownsampleStrategy} option that
   * will be used to calculate the sample size to use to downsample an image given the original and
   * target dimensions of the image.
   *
   * @deprecated Use {@link DownsampleStrategy#OPTION} directly instead.
   */
  @Deprecated
  public static final Option<DownsampleStrategy> DOWNSAMPLE_STRATEGY = DownsampleStrategy.OPTION;
  /**
   * Ensure that the size of the bitmap is fixed to the requested width and height of the resource
   * from the caller. The final resource dimensions may differ from the requested width and height,
   * and thus setting this to true may result in the bitmap size differing from the resource
   * dimensions.
   *
   * <p>This can be used as a performance optimization for KitKat and above by fixing the size of
   * the bitmap for a collection of requested resources so that the bitmap pool will not need to
   * allocate new bitmaps for images of different sizes.
   */
  // Public API
  @SuppressWarnings("WeakerAccess")
  public static final Option<Boolean> FIX_BITMAP_SIZE_TO_REQUESTED_DIMENSIONS =
      Option.memory("com.bumptech.glide.load.resource.bitmap.Downsampler.FixBitmapSize", false);

  /**
   * Indicates that it's safe or unsafe to decode {@link Bitmap}s with {@link
   * Bitmap.Config#HARDWARE}.
   *
   * <p>Callers should almost never set this value to {@code true} manually. Glide will already do
   * so when Glide believes it's safe to do (when no transformations are applied). Instead, callers
   * can set this value to {@code false} to prevent Glide from decoding hardware bitmaps if Glide is
   * unable to detect that hardware bitmaps are unsafe. For example, you should set this to {@code
   * false} if you plan to draw it to a software {@link android.graphics.Canvas} or if you plan to
   * inspect the {@link Bitmap}s pixels with {@link Bitmap#getPixel(int, int)} or {@link
   * Bitmap#getPixels(int[], int, int, int, int, int, int)}.
   *
   * <p>Callers can disable hardware {@link Bitmap}s for all loads using {@link
   * com.bumptech.glide.GlideBuilder#setDefaultRequestOptions(RequestOptions)}.
   *
   * <p>This option is ignored unless we're on Android O+.
   */
  public static final Option<Boolean> ALLOW_HARDWARE_CONFIG =
      Option.memory(
          "com.bumptech.glide.load.resource.bitmap.Downsampler.AllowHardwareDecode", false);

  private static final String WBMP_MIME_TYPE = "image/vnd.wap.wbmp";
  private static final String ICO_MIME_TYPE = "image/x-ico";
  private static final Set<String> NO_DOWNSAMPLE_PRE_N_MIME_TYPES =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(WBMP_MIME_TYPE, ICO_MIME_TYPE)));
  private static final DecodeCallbacks EMPTY_CALLBACKS =
      new DecodeCallbacks() {
        @Override
        public void onObtainBounds() {
          // Do nothing.
        }

        @Override
        public void onDecodeComplete(BitmapPool bitmapPool, Bitmap downsampled) {
          // Do nothing.
        }
      };
  private static final Set<ImageHeaderParser.ImageType> TYPES_THAT_USE_POOL_PRE_KITKAT =
      Collections.unmodifiableSet(
          EnumSet.of(
              ImageHeaderParser.ImageType.JPEG,
              ImageHeaderParser.ImageType.PNG_A,
              ImageHeaderParser.ImageType.PNG));
  private static final Queue<BitmapFactory.Options> OPTIONS_QUEUE = Util.createQueue(0);

  private final BitmapPool bitmapPool;
  private final DisplayMetrics displayMetrics;
  private final ArrayPool byteArrayPool;
  private final List<ImageHeaderParser> parsers;
  private final HardwareConfigState hardwareConfigState = HardwareConfigState.getInstance();

  public Downsampler(
      List<ImageHeaderParser> parsers,
      DisplayMetrics displayMetrics,
      BitmapPool bitmapPool,
      ArrayPool byteArrayPool) {
    this.parsers = parsers;
    this.displayMetrics = Preconditions.checkNotNull(displayMetrics);
    this.bitmapPool = Preconditions.checkNotNull(bitmapPool);
    this.byteArrayPool = Preconditions.checkNotNull(byteArrayPool);
  }

  public boolean handles(@SuppressWarnings("unused") InputStream is) {
    // We expect Downsampler to handle any available type Android supports.
    return true;
  }

  public boolean handles(@SuppressWarnings("unused") ByteBuffer byteBuffer) {
    // We expect downsampler to handle any available type Android supports.
    return true;
  }

  public boolean handles(@SuppressWarnings("unused") ParcelFileDescriptor source) {
    return ParcelFileDescriptorRewinder.isSupported();
  }

  /**
   * Returns a Bitmap decoded from the given {@link InputStream} that is rotated to match any EXIF
   * data present in the stream and that is downsampled according to the given dimensions and any
   * provided {@link com.bumptech.glide.load.resource.bitmap.DownsampleStrategy} option.
   *
   * @see #decode(InputStream, int, int, Options, DecodeCallbacks)
   */
  public Resource<Bitmap> decode(InputStream is, int outWidth, int outHeight, Options options)
      throws IOException {
    return decode(is, outWidth, outHeight, options, EMPTY_CALLBACKS);
  }

  /**
   * Returns a Bitmap decoded from the given {@link InputStream} that is rotated to match any EXIF
   * data present in the stream and that is downsampled according to the given dimensions and any
   * provided {@link com.bumptech.glide.load.resource.bitmap.DownsampleStrategy} option.
   *
   * <p>If a Bitmap is present in the {@link
   * com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} whose dimensions exactly match those
   * of the image for the given InputStream is available, the operation is much less expensive in
   * terms of memory.
   *
   * @param is An {@link InputStream} to the data for the image.
   * @param requestedWidth The width the final image should be close to.
   * @param requestedHeight The height the final image should be close to.
   * @param options A set of options that may contain one or more supported options that influence
   *     how a Bitmap will be decoded from the given stream.
   * @param callbacks A set of callbacks allowing callers to optionally respond to various
   *     significant events during the decode process.
   * @return A new bitmap containing the image from the given InputStream, or recycle if recycle is
   *     not null.
   */
  public Resource<Bitmap> decode(
      InputStream is,
      int requestedWidth,
      int requestedHeight,
      Options options,
      DecodeCallbacks callbacks)
      throws IOException {
    return decode(
        new ImageReader.InputStreamImageReader(is, parsers, byteArrayPool),
        requestedWidth,
        requestedHeight,
        options,
        callbacks);
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  public Resource<Bitmap> decode(
      ParcelFileDescriptor parcelFileDescriptor, int outWidth, int outHeight, Options options)
      throws IOException {
    return decode(
        new ImageReader.ParcelFileDescriptorImageReader(
            parcelFileDescriptor, parsers, byteArrayPool),
        outWidth,
        outHeight,
        options,
        EMPTY_CALLBACKS);
  }

  private Resource<Bitmap> decode(
      ImageReader imageReader,
      int requestedWidth,
      int requestedHeight,
      Options options,
      DecodeCallbacks callbacks)
      throws IOException {
    byte[] bytesForOptions = byteArrayPool.get(ArrayPool.STANDARD_BUFFER_SIZE_BYTES, byte[].class);
    BitmapFactory.Options bitmapFactoryOptions = getDefaultOptions();
    bitmapFactoryOptions.inTempStorage = bytesForOptions;

    DecodeFormat decodeFormat = options.get(DECODE_FORMAT);
    PreferredColorSpace preferredColorSpace = options.get(PREFERRED_COLOR_SPACE);
    DownsampleStrategy downsampleStrategy = options.get(DownsampleStrategy.OPTION);
    boolean fixBitmapToRequestedDimensions = options.get(FIX_BITMAP_SIZE_TO_REQUESTED_DIMENSIONS);
    boolean isHardwareConfigAllowed =
        options.get(ALLOW_HARDWARE_CONFIG) != null && options.get(ALLOW_HARDWARE_CONFIG);

    try {
      Bitmap result =
          decodeFromWrappedStreams(
              imageReader,
              bitmapFactoryOptions,
              downsampleStrategy,
              decodeFormat,
              preferredColorSpace,
              isHardwareConfigAllowed,
              requestedWidth,
              requestedHeight,
              fixBitmapToRequestedDimensions,
              callbacks);
      return BitmapResource.obtain(result, bitmapPool);
    } finally {
      releaseOptions(bitmapFactoryOptions);
      byteArrayPool.put(bytesForOptions);
    }
  }

  private Bitmap decodeFromWrappedStreams(
      ImageReader imageReader,
      BitmapFactory.Options options,
      DownsampleStrategy downsampleStrategy,
      DecodeFormat decodeFormat,
      PreferredColorSpace preferredColorSpace,
      boolean isHardwareConfigAllowed,
      int requestedWidth,
      int requestedHeight,
      boolean fixBitmapToRequestedDimensions,
      DecodeCallbacks callbacks)
      throws IOException {
    long startTime = LogTime.getLogTime();

    int[] sourceDimensions = getDimensions(imageReader, options, callbacks, bitmapPool);
    int sourceWidth = sourceDimensions[0];
    int sourceHeight = sourceDimensions[1];
    String sourceMimeType = options.outMimeType;

    // If we failed to obtain the image dimensions, we may end up with an incorrectly sized Bitmap,
    // so we want to use a mutable Bitmap type. One way this can happen is if the image header is so
    // large (10mb+) that our attempt to use inJustDecodeBounds fails and we're forced to decode the
    // full size image.
    if (sourceWidth == -1 || sourceHeight == -1) {
      isHardwareConfigAllowed = false;
    }

    int orientation = imageReader.getImageOrientation();
    int degreesToRotate = TransformationUtils.getExifOrientationDegrees(orientation);
    boolean isExifOrientationRequired = TransformationUtils.isExifOrientationRequired(orientation);

    int targetWidth =
        requestedWidth == Target.SIZE_ORIGINAL
            ? (isRotationRequired(degreesToRotate) ? sourceHeight : sourceWidth)
            : requestedWidth;
    int targetHeight =
        requestedHeight == Target.SIZE_ORIGINAL
            ? (isRotationRequired(degreesToRotate) ? sourceWidth : sourceHeight)
            : requestedHeight;

    ImageType imageType = imageReader.getImageType();

    calculateScaling(
        imageType,
        imageReader,
        callbacks,
        bitmapPool,
        downsampleStrategy,
        degreesToRotate,
        sourceWidth,
        sourceHeight,
        targetWidth,
        targetHeight,
        options);
    calculateConfig(
        imageReader,
        decodeFormat,
        isHardwareConfigAllowed,
        isExifOrientationRequired,
        options,
        targetWidth,
        targetHeight);

    boolean isKitKatOrGreater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    // Prior to KitKat, the inBitmap size must exactly match the size of the bitmap we're decoding.
    if ((options.inSampleSize == 1 || isKitKatOrGreater) && shouldUsePool(imageType)) {
      int expectedWidth;
      int expectedHeight;
      if (sourceWidth >= 0
          && sourceHeight >= 0
          && fixBitmapToRequestedDimensions
          && isKitKatOrGreater) {
        expectedWidth = targetWidth;
        expectedHeight = targetHeight;
      } else {
        float densityMultiplier =
            isScaling(options) ? (float) options.inTargetDensity / options.inDensity : 1f;
        int sampleSize = options.inSampleSize;
        int downsampledWidth = (int) Math.ceil(sourceWidth / (float) sampleSize);
        int downsampledHeight = (int) Math.ceil(sourceHeight / (float) sampleSize);
        expectedWidth = Math.round(downsampledWidth * densityMultiplier);
        expectedHeight = Math.round(downsampledHeight * densityMultiplier);

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
          Log.v(
              TAG,
              "Calculated target ["
                  + expectedWidth
                  + "x"
                  + expectedHeight
                  + "] for source"
                  + " ["
                  + sourceWidth
                  + "x"
                  + sourceHeight
                  + "]"
                  + ", sampleSize: "
                  + sampleSize
                  + ", targetDensity: "
                  + options.inTargetDensity
                  + ", density: "
                  + options.inDensity
                  + ", density multiplier: "
                  + densityMultiplier);
        }
      }
      // If this isn't an image, or BitmapFactory was unable to parse the size, width and height
      // will be -1 here.
      if (expectedWidth > 0 && expectedHeight > 0) {
        setInBitmap(options, bitmapPool, expectedWidth, expectedHeight);
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      boolean isP3Eligible =
          preferredColorSpace == PreferredColorSpace.DISPLAY_P3
              && options.outColorSpace != null
              && options.outColorSpace.isWideGamut();
      options.inPreferredColorSpace =
          ColorSpace.get(isP3Eligible ? ColorSpace.Named.DISPLAY_P3 : ColorSpace.Named.SRGB);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      options.inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
    }

    Bitmap downsampled = decodeStream(imageReader, options, callbacks, bitmapPool);
    callbacks.onDecodeComplete(bitmapPool, downsampled);

    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      logDecode(
          sourceWidth,
          sourceHeight,
          sourceMimeType,
          options,
          downsampled,
          requestedWidth,
          requestedHeight,
          startTime);
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

  private static void calculateScaling(
      ImageType imageType,
      ImageReader imageReader,
      DecodeCallbacks decodeCallbacks,
      BitmapPool bitmapPool,
      DownsampleStrategy downsampleStrategy,
      int degreesToRotate,
      int sourceWidth,
      int sourceHeight,
      int targetWidth,
      int targetHeight,
      BitmapFactory.Options options)
      throws IOException {
    // We can't downsample source content if we can't determine its dimensions.
    if (sourceWidth <= 0 || sourceHeight <= 0) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(
            TAG,
            "Unable to determine dimensions for: "
                + imageType
                + " with target ["
                + targetWidth
                + "x"
                + targetHeight
                + "]");
      }
      return;
    }

    int orientedSourceWidth = sourceWidth;
    int orientedSourceHeight = sourceHeight;
    // If we're rotating the image +-90 degrees, we need to downsample accordingly so the image
    // width is decreased to near our target's height and the image height is decreased to near
    // our target width.
    //noinspection SuspiciousNameCombination
    if (isRotationRequired(degreesToRotate)) {
      orientedSourceWidth = sourceHeight;
      orientedSourceHeight = sourceWidth;
    }

    final float exactScaleFactor =
        downsampleStrategy.getScaleFactor(
            orientedSourceWidth, orientedSourceHeight, targetWidth, targetHeight);

    if (exactScaleFactor <= 0f) {
      throw new IllegalArgumentException(
          "Cannot scale with factor: "
              + exactScaleFactor
              + " from: "
              + downsampleStrategy
              + ", source: ["
              + sourceWidth
              + "x"
              + sourceHeight
              + "]"
              + ", target: ["
              + targetWidth
              + "x"
              + targetHeight
              + "]");
    }

    SampleSizeRounding rounding =
        downsampleStrategy.getSampleSizeRounding(
            orientedSourceWidth, orientedSourceHeight, targetWidth, targetHeight);
    if (rounding == null) {
      throw new IllegalArgumentException("Cannot round with null rounding");
    }

    int outWidth = round(exactScaleFactor * orientedSourceWidth);
    int outHeight = round(exactScaleFactor * orientedSourceHeight);

    int widthScaleFactor = orientedSourceWidth / outWidth;
    int heightScaleFactor = orientedSourceHeight / outHeight;

    // TODO: This isn't really right for both CenterOutside and CenterInside. Consider allowing
    // DownsampleStrategy to pick, or trying to do something more sophisticated like picking the
    // scale factor that leads to an exact match.
    int scaleFactor =
        rounding == SampleSizeRounding.MEMORY
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

    // Here we mimic framework logic for determining how inSampleSize division is rounded on various
    // versions of Android. The logic here has been tested on emulators for Android versions 15-26.
    // PNG - Always uses floor
    // JPEG - Always uses ceiling
    // Webp - Prior to N, always uses floor. At and after N, always uses round.
    options.inSampleSize = powerOfTwoSampleSize;
    int powerOfTwoWidth;
    int powerOfTwoHeight;
    if (imageType == ImageType.JPEG) {
      // libjpegturbo can downsample up to a sample size of 8. libjpegturbo uses ceiling to round.
      // After libjpegturbo's native rounding, skia does a secondary scale using floor
      // (integer division). Here we replicate that logic.
      int nativeScaling = Math.min(powerOfTwoSampleSize, 8);
      powerOfTwoWidth = (int) Math.ceil(orientedSourceWidth / (float) nativeScaling);
      powerOfTwoHeight = (int) Math.ceil(orientedSourceHeight / (float) nativeScaling);
      int secondaryScaling = powerOfTwoSampleSize / 8;
      if (secondaryScaling > 0) {
        powerOfTwoWidth = powerOfTwoWidth / secondaryScaling;
        powerOfTwoHeight = powerOfTwoHeight / secondaryScaling;
      }
    } else if (imageType == ImageType.PNG || imageType == ImageType.PNG_A) {
      powerOfTwoWidth = (int) Math.floor(orientedSourceWidth / (float) powerOfTwoSampleSize);
      powerOfTwoHeight = (int) Math.floor(orientedSourceHeight / (float) powerOfTwoSampleSize);
    } else if (imageType == ImageType.WEBP || imageType == ImageType.WEBP_A) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        powerOfTwoWidth = Math.round(orientedSourceWidth / (float) powerOfTwoSampleSize);
        powerOfTwoHeight = Math.round(orientedSourceHeight / (float) powerOfTwoSampleSize);
      } else {
        powerOfTwoWidth = (int) Math.floor(orientedSourceWidth / (float) powerOfTwoSampleSize);
        powerOfTwoHeight = (int) Math.floor(orientedSourceHeight / (float) powerOfTwoSampleSize);
      }
    } else if (orientedSourceWidth % powerOfTwoSampleSize != 0
        || orientedSourceHeight % powerOfTwoSampleSize != 0) {
      // If we're not confident the image is in one of our types, fall back to checking the
      // dimensions again. inJustDecodeBounds decodes do obey inSampleSize.
      int[] dimensions = getDimensions(imageReader, options, decodeCallbacks, bitmapPool);
      // Power of two downsampling in BitmapFactory uses a variety of random factors to determine
      // rounding that we can't reliably replicate for all image formats. Use ceiling here to make
      // sure that we at least provide a Bitmap that's large enough to fit the content we're going
      // to load.
      powerOfTwoWidth = dimensions[0];
      powerOfTwoHeight = dimensions[1];
    } else {
      powerOfTwoWidth = orientedSourceWidth / powerOfTwoSampleSize;
      powerOfTwoHeight = orientedSourceHeight / powerOfTwoSampleSize;
    }

    double adjustedScaleFactor =
        downsampleStrategy.getScaleFactor(
            powerOfTwoWidth, powerOfTwoHeight, targetWidth, targetHeight);

    // Density scaling is only supported if inBitmap is null prior to KitKat. Avoid setting
    // densities here so we calculate the final Bitmap size correctly.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      options.inTargetDensity = adjustTargetDensityForError(adjustedScaleFactor);
      options.inDensity = getDensityMultiplier(adjustedScaleFactor);
    }
    if (isScaling(options)) {
      options.inScaled = true;
    } else {
      options.inDensity = options.inTargetDensity = 0;
    }

    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      Log.v(
          TAG,
          "Calculate scaling"
              + ", source: ["
              + sourceWidth
              + "x"
              + sourceHeight
              + "]"
              + ", degreesToRotate: "
              + degreesToRotate
              + ", target: ["
              + targetWidth
              + "x"
              + targetHeight
              + "]"
              + ", power of two scaled: ["
              + powerOfTwoWidth
              + "x"
              + powerOfTwoHeight
              + "]"
              + ", exact scale factor: "
              + exactScaleFactor
              + ", power of 2 sample size: "
              + powerOfTwoSampleSize
              + ", adjusted scale factor: "
              + adjustedScaleFactor
              + ", target density: "
              + options.inTargetDensity
              + ", density: "
              + options.inDensity);
    }
  }

  /**
   * BitmapFactory calculates the density scale factor as a float. This introduces some non-trivial
   * error. This method attempts to account for that error by adjusting the inTargetDensity so that
   * the final scale factor is as close to our target as possible.
   */
  private static int adjustTargetDensityForError(double adjustedScaleFactor) {
    int densityMultiplier = getDensityMultiplier(adjustedScaleFactor);
    int targetDensity = round(densityMultiplier * adjustedScaleFactor);
    float scaleFactorWithError = targetDensity / (float) densityMultiplier;
    double difference = adjustedScaleFactor / scaleFactorWithError;
    return round(difference * targetDensity);
  }

  private static int getDensityMultiplier(double adjustedScaleFactor) {
    return (int)
        Math.round(
            Integer.MAX_VALUE
                * (adjustedScaleFactor <= 1D ? adjustedScaleFactor : 1 / adjustedScaleFactor));
  }

  // This is weird, but it matches the logic in a bunch of Android views/framework classes for
  // rounding.
  private static int round(double value) {
    return (int) (value + 0.5d);
  }

  private boolean shouldUsePool(ImageType imageType) {
    // On KitKat+, any bitmap (of a given config) can be used to decode any other bitmap
    // (with the same config).
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      return true;
    }

    // We cannot reuse bitmaps when decoding images that are not PNG or JPG prior to KitKat.
    // See: https://groups.google.com/forum/#!msg/android-developers/Mp0MFVFi1Fo/e8ZQ9FGdWdEJ
    return TYPES_THAT_USE_POOL_PRE_KITKAT.contains(imageType);
  }

  @SuppressWarnings("deprecation")
  private void calculateConfig(
      ImageReader imageReader,
      DecodeFormat format,
      boolean isHardwareConfigAllowed,
      boolean isExifOrientationRequired,
      BitmapFactory.Options optionsWithScaling,
      int targetWidth,
      int targetHeight) {

    if (hardwareConfigState.setHardwareConfigIfAllowed(
        targetWidth,
        targetHeight,
        optionsWithScaling,
        isHardwareConfigAllowed,
        isExifOrientationRequired)) {
      return;
    }

    // Changing configs can cause skewing on 4.1, see issue #128.
    if (format == DecodeFormat.PREFER_ARGB_8888
        || Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
      optionsWithScaling.inPreferredConfig = Bitmap.Config.ARGB_8888;
      return;
    }

    boolean hasAlpha = false;
    try {
      hasAlpha = imageReader.getImageType().hasAlpha();
    } catch (IOException e) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(
            TAG,
            "Cannot determine whether the image has alpha or not from header"
                + ", format "
                + format,
            e);
      }
    }

    optionsWithScaling.inPreferredConfig =
        hasAlpha ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
    if (optionsWithScaling.inPreferredConfig == Config.RGB_565) {
      optionsWithScaling.inDither = true;
    }
  }

  /**
   * A method for getting the dimensions of an image from the given InputStream.
   *
   * @param imageReader The {@link ImageReader} representing the image.
   * @param options The options to pass to {@link BitmapFactory#decodeStream(java.io.InputStream,
   *     android.graphics.Rect, android.graphics.BitmapFactory.Options)}.
   * @return an array containing the dimensions of the image in the form {width, height}.
   */
  private static int[] getDimensions(
      ImageReader imageReader,
      BitmapFactory.Options options,
      DecodeCallbacks decodeCallbacks,
      BitmapPool bitmapPool)
      throws IOException {
    options.inJustDecodeBounds = true;
    decodeStream(imageReader, options, decodeCallbacks, bitmapPool);
    options.inJustDecodeBounds = false;
    return new int[] {options.outWidth, options.outHeight};
  }

  private static Bitmap decodeStream(
      ImageReader imageReader,
      BitmapFactory.Options options,
      DecodeCallbacks callbacks,
      BitmapPool bitmapPool)
      throws IOException {
    if (!options.inJustDecodeBounds) {
      // Once we've read the image header, we no longer need to allow the buffer to expand in
      // size. To avoid unnecessary allocations reading image data, we fix the mark limit so that it
      // is no larger than our current buffer size here. We need to do so immediately before
      // decoding the full image to avoid having our mark limit overridden by other calls to
      // mark and reset. See issue #225.
      callbacks.onObtainBounds();
      imageReader.stopGrowingBuffers();
    }

    // BitmapFactory.Options out* variables are reset by most calls to decodeStream, successful or
    // otherwise, so capture here in case we log below.
    int sourceWidth = options.outWidth;
    int sourceHeight = options.outHeight;
    String outMimeType = options.outMimeType;
    final Bitmap result;
    TransformationUtils.getBitmapDrawableLock().lock();
    try {
      result = imageReader.decodeBitmap(options);
    } catch (IllegalArgumentException e) {
      IOException bitmapAssertionException =
          newIoExceptionForInBitmapAssertion(e, sourceWidth, sourceHeight, outMimeType, options);
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(
            TAG,
            "Failed to decode with inBitmap, trying again without Bitmap re-use",
            bitmapAssertionException);
      }
      if (options.inBitmap != null) {
        try {
          bitmapPool.put(options.inBitmap);
          options.inBitmap = null;
          return decodeStream(imageReader, options, callbacks, bitmapPool);
        } catch (IOException resetException) {
          throw bitmapAssertionException;
        }
      }
      throw bitmapAssertionException;
    } finally {
      TransformationUtils.getBitmapDrawableLock().unlock();
    }

    return result;
  }

  private static boolean isScaling(BitmapFactory.Options options) {
    return options.inTargetDensity > 0
        && options.inDensity > 0
        && options.inTargetDensity != options.inDensity;
  }

  private static void logDecode(
      int sourceWidth,
      int sourceHeight,
      String outMimeType,
      BitmapFactory.Options options,
      Bitmap result,
      int requestedWidth,
      int requestedHeight,
      long startTime) {
    Log.v(
        TAG,
        "Decoded "
            + getBitmapString(result)
            + " from ["
            + sourceWidth
            + "x"
            + sourceHeight
            + "] "
            + outMimeType
            + " with inBitmap "
            + getInBitmapString(options)
            + " for ["
            + requestedWidth
            + "x"
            + requestedHeight
            + "]"
            + ", sample size: "
            + options.inSampleSize
            + ", density: "
            + options.inDensity
            + ", target density: "
            + options.inTargetDensity
            + ", thread: "
            + Thread.currentThread().getName()
            + ", duration: "
            + LogTime.getElapsedMillis(startTime));
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

    String sizeString =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
            ? " (" + bitmap.getAllocationByteCount() + ")"
            : "";
    return "["
        + bitmap.getWidth()
        + "x"
        + bitmap.getHeight()
        + "] "
        + bitmap.getConfig()
        + sizeString;
  }

  // BitmapFactory throws an IllegalArgumentException if any error occurs attempting to decode a
  // file when inBitmap is non-null, including those caused by partial or corrupt data. We still log
  // the error because the IllegalArgumentException is supposed to catch errors reusing Bitmaps, so
  // want some useful log output. In most cases this can be safely treated as a normal IOException.
  private static IOException newIoExceptionForInBitmapAssertion(
      IllegalArgumentException e,
      int outWidth,
      int outHeight,
      String outMimeType,
      BitmapFactory.Options options) {
    return new IOException(
        "Exception decoding bitmap"
            + ", outWidth: "
            + outWidth
            + ", outHeight: "
            + outHeight
            + ", outMimeType: "
            + outMimeType
            + ", inBitmap: "
            + getInBitmapString(options),
        e);
  }

  @SuppressWarnings("PMD.CollapsibleIfStatements")
  @TargetApi(Build.VERSION_CODES.O)
  private static void setInBitmap(
      BitmapFactory.Options options, BitmapPool bitmapPool, int width, int height) {
    @Nullable Bitmap.Config expectedConfig = null;
    // Avoid short circuiting, it appears to break on some devices.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (options.inPreferredConfig == Config.HARDWARE) {
        return;
      }
      // On API 26 outConfig may be null for some images even if the image is valid, can be decoded
      // and outWidth/outHeight/outColorSpace are populated (see b/71513049).
      expectedConfig = options.outConfig;
    }

    if (expectedConfig == null) {
      // We're going to guess that BitmapFactory will return us the config we're requesting. This
      // isn't always the case, even though our guesses tend to be conservative and prefer configs
      // of larger sizes so that the Bitmap will fit our image anyway. If we're wrong here and the
      // config we choose is too small, our initial decode will fail, but we will retry with no
      // inBitmap which will succeed so if we're wrong here, we're less efficient but still correct.
      expectedConfig = options.inPreferredConfig;
    }
    // BitmapFactory will clear out the Bitmap before writing to it, so getDirty is safe.
    options.inBitmap = bitmapPool.getDirty(width, height, expectedConfig);
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

  @SuppressWarnings("deprecation")
  private static void resetOptions(BitmapFactory.Options decodeBitmapOptions) {
    decodeBitmapOptions.inTempStorage = null;
    decodeBitmapOptions.inDither = false;
    decodeBitmapOptions.inScaled = false;
    decodeBitmapOptions.inSampleSize = 1;
    decodeBitmapOptions.inPreferredConfig = null;
    decodeBitmapOptions.inJustDecodeBounds = false;
    decodeBitmapOptions.inDensity = 0;
    decodeBitmapOptions.inTargetDensity = 0;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      decodeBitmapOptions.inPreferredColorSpace = null;
      decodeBitmapOptions.outColorSpace = null;
      decodeBitmapOptions.outConfig = null;
    }
    decodeBitmapOptions.outWidth = 0;
    decodeBitmapOptions.outHeight = 0;
    decodeBitmapOptions.outMimeType = null;
    decodeBitmapOptions.inBitmap = null;
    decodeBitmapOptions.inMutable = true;
  }

  /** Callbacks for key points during decodes. */
  public interface DecodeCallbacks {
    void onObtainBounds();

    void onDecodeComplete(BitmapPool bitmapPool, Bitmap downsampled) throws IOException;
  }

  private static boolean isRotationRequired(int degreesToRotate) {
    return degreesToRotate == 90 || degreesToRotate == 270;
  }
}
