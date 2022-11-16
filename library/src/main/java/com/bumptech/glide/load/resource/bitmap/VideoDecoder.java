package com.bumptech.glide.load.resource.bitmap;

import android.annotation.TargetApi;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaDataSource;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.request.target.Target;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Decodes video data to Bitmaps from {@link ParcelFileDescriptor}s and {@link
 * AssetFileDescriptor}s.
 *
 * @param <T> The type of data, currently either {@link ParcelFileDescriptor} or {@link
 *     AssetFileDescriptor}.
 */
public class VideoDecoder<T> implements ResourceDecoder<T, Bitmap> {
  private static final String TAG = "VideoDecoder";

  /**
   * A constant indicating we should use whatever frame we consider best, frequently not the first
   * frame.
   */
  public static final long DEFAULT_FRAME = -1;

  /** Matches the behavior of {@link MediaMetadataRetriever#getFrameAtTime(long)}. */
  @VisibleForTesting
  static final int DEFAULT_FRAME_OPTION = MediaMetadataRetriever.OPTION_CLOSEST_SYNC;

  /**
   * A long indicating the time position (in microseconds) of the target frame which will be
   * retrieved. {@link android.media.MediaMetadataRetriever#getFrameAtTime(long)} is used to extract
   * the video frame.
   *
   * <p>When retrieving the frame at the given time position, there is no guarantee that the data
   * source has a frame located at the position. When this happens, a frame nearby will be returned.
   * If the long is negative, time position and option will ignored, and any frame that the
   * implementation considers as representative may be returned.
   */
  public static final Option<Long> TARGET_FRAME =
      Option.disk(
          "com.bumptech.glide.load.resource.bitmap.VideoBitmapDecode.TargetFrame",
          DEFAULT_FRAME,
          new Option.CacheKeyUpdater<Long>() {
            private final ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);

            @Override
            public void update(
                @NonNull byte[] keyBytes,
                @NonNull Long value,
                @NonNull MessageDigest messageDigest) {
              messageDigest.update(keyBytes);
              synchronized (buffer) {
                buffer.position(0);
                messageDigest.update(buffer.putLong(value).array());
              }
            }
          });

  /**
   * An integer indicating the frame option used to retrieve a target frame.
   *
   * <p>This option will be ignored if {@link #TARGET_FRAME} is not set or is set to {@link
   * #DEFAULT_FRAME}.
   *
   * @see MediaMetadataRetriever#getFrameAtTime(long, int)
   */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  public static final Option<Integer> FRAME_OPTION =
      Option.disk(
          "com.bumptech.glide.load.resource.bitmap.VideoBitmapDecode.FrameOption",
          /* defaultValue= */ MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
          new Option.CacheKeyUpdater<Integer>() {
            private final ByteBuffer buffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);

            @Override
            public void update(
                @NonNull byte[] keyBytes,
                @NonNull Integer value,
                @NonNull MessageDigest messageDigest) {
              //noinspection ConstantConditions public API, people could have been doing it wrong
              if (value == null) {
                return;
              }
              messageDigest.update(keyBytes);
              synchronized (buffer) {
                buffer.position(0);
                messageDigest.update(buffer.putInt(value).array());
              }
            }
          });

  private static final MediaMetadataRetrieverFactory DEFAULT_FACTORY =
      new MediaMetadataRetrieverFactory();

  /**
   * List of Pixel Android T build id prefixes missing a fix for HDR video with 180 deg rotations
   * having doubly-rotated thumbnails.
   *
   * <p>More recent Android T builds should have the fix.
   */
  private static final List<String> PIXEL_T_BUILD_ID_PREFIXES_REQUIRING_HDR_180_ROTATION_FIX =
      Collections.unmodifiableList(Arrays.asList("TP1A", "TD1A.220804.031"));

  private static final String WEBM_MIME_TYPE = "video/webm";

  private final MediaInitializer<T> initializer;
  private final BitmapPool bitmapPool;
  private final MediaMetadataRetrieverFactory factory;

  public static ResourceDecoder<AssetFileDescriptor, Bitmap> asset(BitmapPool bitmapPool) {
    return new VideoDecoder<>(bitmapPool, new AssetFileDescriptorInitializer());
  }

  public static ResourceDecoder<ParcelFileDescriptor, Bitmap> parcel(BitmapPool bitmapPool) {
    return new VideoDecoder<>(bitmapPool, new ParcelFileDescriptorInitializer());
  }

  @RequiresApi(api = VERSION_CODES.M)
  public static ResourceDecoder<ByteBuffer, Bitmap> byteBuffer(BitmapPool bitmapPool) {
    return new VideoDecoder<>(bitmapPool, new ByteBufferInitializer());
  }

  VideoDecoder(BitmapPool bitmapPool, MediaInitializer<T> initializer) {
    this(bitmapPool, initializer, DEFAULT_FACTORY);
  }

  @VisibleForTesting
  VideoDecoder(
      BitmapPool bitmapPool,
      MediaInitializer<T> initializer,
      MediaMetadataRetrieverFactory factory) {
    this.bitmapPool = bitmapPool;
    this.initializer = initializer;
    this.factory = factory;
  }

  @Override
  public boolean handles(@NonNull T data, @NonNull Options options) {
    // Calling setDataSource is expensive so avoid doing so unless we're actually called.
    // For non-videos this isn't any cheaper, but for videos it saves the redundant call and
    // 50-100ms.
    return true;
  }

  @Override
  public Resource<Bitmap> decode(
      @NonNull T resource, int outWidth, int outHeight, @NonNull Options options)
      throws IOException {
    long frameTimeMicros = options.get(TARGET_FRAME);
    if (frameTimeMicros < 0 && frameTimeMicros != DEFAULT_FRAME) {
      throw new IllegalArgumentException(
          "Requested frame must be non-negative, or DEFAULT_FRAME, given: " + frameTimeMicros);
    }
    Integer frameOption = options.get(FRAME_OPTION);
    if (frameOption == null) {
      frameOption = DEFAULT_FRAME_OPTION;
    }
    DownsampleStrategy downsampleStrategy = options.get(DownsampleStrategy.OPTION);
    if (downsampleStrategy == null) {
      downsampleStrategy = DownsampleStrategy.DEFAULT;
    }

    final Bitmap result;
    MediaMetadataRetriever mediaMetadataRetriever = factory.build();
    try {
      initializer.initializeRetriever(mediaMetadataRetriever, resource);
      result =
          decodeFrame(
              resource,
              mediaMetadataRetriever,
              frameTimeMicros,
              frameOption,
              outWidth,
              outHeight,
              downsampleStrategy);
    } finally {
      if (Build.VERSION.SDK_INT >= VERSION_CODES.Q) {
        mediaMetadataRetriever.close();
      } else {
        mediaMetadataRetriever.release();
      }
    }

    return BitmapResource.obtain(result, bitmapPool);
  }

  @Nullable
  private Bitmap decodeFrame(
      @NonNull T resource,
      MediaMetadataRetriever mediaMetadataRetriever,
      long frameTimeMicros,
      int frameOption,
      int outWidth,
      int outHeight,
      DownsampleStrategy strategy) {
    if (isUnsupportedFormat(resource, mediaMetadataRetriever)) {
      throw new IllegalStateException("Cannot decode VP8 video on CrOS.");
    }

    Bitmap result = null;
    // Arguably we should handle the case where just width or just height is set to
    // Target.SIZE_ORIGINAL. Up to and including OMR1, MediaMetadataRetriever defaults to setting
    // the dimensions to the display width and height if they aren't specified (ie
    // getScaledFrameAtTime is not used). Given that this is an optimization only if
    // Target.SIZE_ORIGINAL is not used and not using getScaledFrameAtTime ever would match the
    // behavior of Glide in all versions of Android prior to OMR1, it's probably fine for now.
    if (Build.VERSION.SDK_INT >= VERSION_CODES.O_MR1
        && outWidth != Target.SIZE_ORIGINAL
        && outHeight != Target.SIZE_ORIGINAL
        && strategy != DownsampleStrategy.NONE) {
      result =
          decodeScaledFrame(
              mediaMetadataRetriever, frameTimeMicros, frameOption, outWidth, outHeight, strategy);
    }

    if (result == null) {
      result = decodeOriginalFrame(mediaMetadataRetriever, frameTimeMicros, frameOption);
    }

    // MediaMetadataRetriever has a bug where HDR videos with 180 deg rotations are rotated twice,
    // causing the output frame to appear upside. This needs to be corrected for all versions of
    // Android until a platform fix lands.
    result = correctHdr180DegVideoFrameOrientation(mediaMetadataRetriever, result);

    // Throwing an exception works better in our error logging than returning null. It shouldn't
    // be expensive because video decoders are attempted after image loads. Video errors are often
    // logged by the framework, so we can also use this error to suggest callers look for the
    // appropriate tags in adb.
    if (result == null) {
      throw new VideoDecoderException();
    }

    return result;
  }

  /**
   * Corrects the orientation of a bitmap extracted from an HDR video with a 180 degree rotation
   * angle.
   *
   * <p>This method will only return a rotated bitmap instead of the input bitmap if
   *
   * <ul>
   *   <li>The Android SDK level is >= R && < T OR the build id is one of T builds without the
   *       platform fix.
   *   <li>The video has a color transfer function with an HLG or ST2084 (PQ) transfer function.
   *   <li>The video has a color standard of BT.2020.
   *   <li>The video has a rotation angle of +/- 180 degrees.
   * </ul>
   */
  private static Bitmap correctHdr180DegVideoFrameOrientation(
      MediaMetadataRetriever mediaMetadataRetriever, Bitmap frame) {
    if (!isHdr180RotationFixRequired()) {
      return frame;
    }
    boolean requiresHdr180RotationFix = false;
    try {
      if (isHDR(mediaMetadataRetriever)) {
        String rotationString =
            mediaMetadataRetriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        int rotation = Integer.parseInt(rotationString);
        requiresHdr180RotationFix = Math.abs(rotation) == 180;
      }
    } catch (NumberFormatException e) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Exception trying to extract HDR transfer function or rotation");
      }
    }

    if (!requiresHdr180RotationFix) {
      return frame;
    }

    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "Applying HDR 180 deg thumbnail correction");
    }
    Matrix rotationMatrix = new Matrix();
    rotationMatrix.postRotate(
        /* degrees= */ 180, frame.getWidth() / 2.0f, frame.getHeight() / 2.0f);
    return Bitmap.createBitmap(
        frame,
        /* x= */ 0,
        /* y= */ 0,
        frame.getWidth(),
        frame.getHeight(),
        rotationMatrix,
        /* filter= */ true);
  }

  @RequiresApi(VERSION_CODES.R)
  private static boolean isHDR(MediaMetadataRetriever mediaMetadataRetriever)
      throws NumberFormatException {
    String colorTransferString =
        mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER);
    String colorStandardString =
        mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_STANDARD);
    int colorTransfer = Integer.parseInt(colorTransferString);
    int colorStandard = Integer.parseInt(colorStandardString);
    // This check needs to match the isHDR check in
    // frameworks/av/media/libstagefright/FrameDecoder.cpp.
    return (colorTransfer == MediaFormat.COLOR_TRANSFER_HLG
            || colorTransfer == MediaFormat.COLOR_TRANSFER_ST2084)
        && colorStandard == MediaFormat.COLOR_STANDARD_BT2020;
  }

  /** Returns true if the build requires a fix for the HDR 180 degree rotation bug. */
  @VisibleForTesting
  static boolean isHdr180RotationFixRequired() {
    // Only pixel devices have android T builds without the framework fix.
    if (Build.MODEL.startsWith("Pixel") && VERSION.SDK_INT == VERSION_CODES.TIRAMISU) {
      return isTBuildRequiringRotationFix();
    } else {
      return VERSION.SDK_INT >= VERSION_CODES.R && VERSION.SDK_INT < VERSION_CODES.TIRAMISU;
    }
  }

  /**
   * Returns true if the build is an Android T build that requires a fix for the HDR 180 degree
   * rotation bug.
   */
  private static boolean isTBuildRequiringRotationFix() {
    for (String buildId : PIXEL_T_BUILD_ID_PREFIXES_REQUIRING_HDR_180_ROTATION_FIX) {
      if (Build.ID.startsWith(buildId)) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  @TargetApi(Build.VERSION_CODES.O_MR1)
  private static Bitmap decodeScaledFrame(
      MediaMetadataRetriever mediaMetadataRetriever,
      long frameTimeMicros,
      int frameOption,
      int outWidth,
      int outHeight,
      DownsampleStrategy strategy) {
    try {
      int originalWidth =
          Integer.parseInt(
              mediaMetadataRetriever.extractMetadata(
                  MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
      int originalHeight =
          Integer.parseInt(
              mediaMetadataRetriever.extractMetadata(
                  MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
      int orientation =
          Integer.parseInt(
              mediaMetadataRetriever.extractMetadata(
                  MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));

      if (orientation == 90 || orientation == 270) {
        int temp = originalWidth;
        //noinspection SuspiciousNameCombination
        originalWidth = originalHeight;
        originalHeight = temp;
      }

      float scaleFactor =
          strategy.getScaleFactor(originalWidth, originalHeight, outWidth, outHeight);

      int decodeWidth = Math.round(scaleFactor * originalWidth);
      int decodeHeight = Math.round(scaleFactor * originalHeight);

      return mediaMetadataRetriever.getScaledFrameAtTime(
          frameTimeMicros, frameOption, decodeWidth, decodeHeight);
    } catch (Throwable t) {
      // This is aggressive, but we'd rather catch errors caused by reading and/or parsing metadata
      // here and fall back to just decoding the frame whenever possible. If the exception is thrown
      // just from decoding the frame, then it will be thrown and exposed to callers by the method
      // below.
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(
            TAG,
            "Exception trying to decode a scaled frame on oreo+, falling back to a fullsize frame",
            t);
      }

      return null;
    }
  }

  private static Bitmap decodeOriginalFrame(
      MediaMetadataRetriever mediaMetadataRetriever, long frameTimeMicros, int frameOption) {
    return mediaMetadataRetriever.getFrameAtTime(frameTimeMicros, frameOption);
  }

  /** Returns true if the format type is unsupported on the device. */
  private boolean isUnsupportedFormat(
      @NonNull T resource, MediaMetadataRetriever mediaMetadataRetriever) {
    // MediaFormat.KEY_MIME check below requires at least JELLY_BEAN
    if (Build.VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN) {
      return false;
    }

    // The primary known problem is vp8 video on ChromeOS (ARC) devices.
    boolean isArc = Build.DEVICE != null && Build.DEVICE.matches(".+_cheets|cheets_.+");
    if (!isArc) {
      return false;
    }

    MediaExtractor mediaExtractor = null;
    try {
      // Include the MediaMetadataRetriever extract in the try block out of an abundance of caution.
      String mimeType =
          mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
      if (!WEBM_MIME_TYPE.equals(mimeType)) {
        return false;
      }

      // Only construct a MediaExtractor for webm files, since the constructor makes a JNI call
      mediaExtractor = new MediaExtractor();
      initializer.initializeExtractor(mediaExtractor, resource);
      int numTracks = mediaExtractor.getTrackCount();
      for (int i = 0; i < numTracks; ++i) {
        MediaFormat mediaformat = mediaExtractor.getTrackFormat(i);
        String trackMimeType = mediaformat.getString(MediaFormat.KEY_MIME);
        if (MediaFormat.MIMETYPE_VIDEO_VP8.equals(trackMimeType)) {
          return true;
        }
      }
    } catch (Throwable t) {
      // Catching everything here out of an abundance of caution
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Exception trying to extract track info for a webm video on CrOS.", t);
      }
    } finally {
      if (mediaExtractor != null) {
        mediaExtractor.release();
      }
    }

    return false;
  }

  @VisibleForTesting
  static class MediaMetadataRetrieverFactory {
    public MediaMetadataRetriever build() {
      return new MediaMetadataRetriever();
    }
  }

  @VisibleForTesting
  interface MediaInitializer<T> {
    void initializeRetriever(MediaMetadataRetriever retriever, T data);

    void initializeExtractor(MediaExtractor extractor, T data) throws IOException;
  }

  private static final class AssetFileDescriptorInitializer
      implements MediaInitializer<AssetFileDescriptor> {

    @Override
    public void initializeRetriever(MediaMetadataRetriever retriever, AssetFileDescriptor data) {
      retriever.setDataSource(data.getFileDescriptor(), data.getStartOffset(), data.getLength());
    }

    @Override
    public void initializeExtractor(MediaExtractor extractor, AssetFileDescriptor data)
        throws IOException {
      extractor.setDataSource(data.getFileDescriptor(), data.getStartOffset(), data.getLength());
    }
  }

  // Visible for VideoBitmapDecoder.
  static final class ParcelFileDescriptorInitializer
      implements MediaInitializer<ParcelFileDescriptor> {

    @Override
    public void initializeRetriever(MediaMetadataRetriever retriever, ParcelFileDescriptor data) {
      retriever.setDataSource(data.getFileDescriptor());
    }

    @Override
    public void initializeExtractor(MediaExtractor extractor, ParcelFileDescriptor data)
        throws IOException {
      extractor.setDataSource(data.getFileDescriptor());
    }
  }

  @RequiresApi(Build.VERSION_CODES.M)
  static final class ByteBufferInitializer implements MediaInitializer<ByteBuffer> {

    @Override
    public void initializeRetriever(MediaMetadataRetriever retriever, final ByteBuffer data) {
      retriever.setDataSource(getMediaDataSource(data));
    }

    @Override
    public void initializeExtractor(MediaExtractor extractor, final ByteBuffer data)
        throws IOException {
      extractor.setDataSource(getMediaDataSource(data));
    }

    private MediaDataSource getMediaDataSource(final ByteBuffer data) {
      return new MediaDataSource() {
        @Override
        public int readAt(long position, byte[] buffer, int offset, int size) {
          if (position >= data.limit()) {
            return -1;
          }
          data.position((int) position);
          int numBytesRead = Math.min(size, data.remaining());
          data.get(buffer, offset, numBytesRead);
          return numBytesRead;
        }

        @Override
        public long getSize() {
          return data.limit();
        }

        @Override
        public void close() {}
      };
    }
  }

  private static final class VideoDecoderException extends RuntimeException {

    private static final long serialVersionUID = -2556382523004027815L;

    VideoDecoderException() {
      super(
          "MediaMetadataRetriever failed to retrieve a frame without throwing, check the adb logs"
              + " for .*MetadataRetriever.* prior to this exception for details");
    }
  }
}
