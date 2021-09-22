package com.bumptech.glide.load.resource.bitmap;

import android.annotation.TargetApi;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.MediaDataSource;
import android.media.MediaMetadataRetriever;
import android.os.Build;
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
          /*defaultValue=*/ MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
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

  private final MediaMetadataRetrieverInitializer<T> initializer;
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

  VideoDecoder(BitmapPool bitmapPool, MediaMetadataRetrieverInitializer<T> initializer) {
    this(bitmapPool, initializer, DEFAULT_FACTORY);
  }

  @VisibleForTesting
  VideoDecoder(
      BitmapPool bitmapPool,
      MediaMetadataRetrieverInitializer<T> initializer,
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
      initializer.initialize(mediaMetadataRetriever, resource);
      result =
          decodeFrame(
              mediaMetadataRetriever,
              frameTimeMicros,
              frameOption,
              outWidth,
              outHeight,
              downsampleStrategy);
    } finally {
      mediaMetadataRetriever.release();
    }

    return BitmapResource.obtain(result, bitmapPool);
  }

  @Nullable
  private static Bitmap decodeFrame(
      MediaMetadataRetriever mediaMetadataRetriever,
      long frameTimeMicros,
      int frameOption,
      int outWidth,
      int outHeight,
      DownsampleStrategy strategy) {
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

    // Throwing an exception works better in our error logging than returning null. It shouldn't
    // be expensive because video decoders are attempted after image loads. Video errors are often
    // logged by the framework, so we can also use this error to suggest callers look for the
    // appropriate tags in adb.
    if (result == null) {
      throw new VideoDecoderException();
    }

    return result;
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

  @VisibleForTesting
  static class MediaMetadataRetrieverFactory {
    public MediaMetadataRetriever build() {
      return new MediaMetadataRetriever();
    }
  }

  @VisibleForTesting
  interface MediaMetadataRetrieverInitializer<T> {
    void initialize(MediaMetadataRetriever retriever, T data);
  }

  private static final class AssetFileDescriptorInitializer
      implements MediaMetadataRetrieverInitializer<AssetFileDescriptor> {

    @Override
    public void initialize(MediaMetadataRetriever retriever, AssetFileDescriptor data) {
      retriever.setDataSource(data.getFileDescriptor(), data.getStartOffset(), data.getLength());
    }
  }

  // Visible for VideoBitmapDecoder.
  static final class ParcelFileDescriptorInitializer
      implements MediaMetadataRetrieverInitializer<ParcelFileDescriptor> {

    @Override
    public void initialize(MediaMetadataRetriever retriever, ParcelFileDescriptor data) {
      retriever.setDataSource(data.getFileDescriptor());
    }
  }

  @RequiresApi(Build.VERSION_CODES.M)
  static final class ByteBufferInitializer
      implements MediaMetadataRetrieverInitializer<ByteBuffer> {

    @Override
    public void initialize(MediaMetadataRetriever retriever, final ByteBuffer data) {
      retriever.setDataSource(
          new MediaDataSource() {
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
          });
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
