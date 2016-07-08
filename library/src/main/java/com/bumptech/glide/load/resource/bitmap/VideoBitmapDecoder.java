package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

/**
 * An {@link com.bumptech.glide.load.ResourceDecoder} that can decode a thumbnail frame
 * {@link android.graphics.Bitmap} from a {@link android.os.ParcelFileDescriptor} containing a
 * video.
 *
 * @see android.media.MediaMetadataRetriever
 */
public class VideoBitmapDecoder implements ResourceDecoder<ParcelFileDescriptor, Bitmap> {
  /**
   * A constant indicating we should use whatever frame we consider best, frequently not the first
   * frame.
   */
  public static final long DEFAULT_FRAME = -1;

  /**
   * A long indicating the time position (in microseconds) of the target frame which will be
   * retrieved. {@link android.media.MediaMetadataRetriever#getFrameAtTime(long)} is used to
   * extract the video frame.
   *
   * <p>When retrieving the frame at the given time position, there is no guarantee that the data
   * source has a frame located at the position. When this happens, a frame nearby will be returned.
   * If the long is negative, time position and option will ignored, and any frame that the
   * implementation considers as representative may be returned.
   */
  public static final Option<Long> TARGET_FRAME = Option.disk(
      "com.bumptech.glide.load.resource.bitmap.VideoBitmapDecode.TargetFrame", DEFAULT_FRAME,
      new Option.CacheKeyUpdater<Long>() {
        private final ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);
        @Override
        public void update(byte[] keyBytes, Long value, MessageDigest messageDigest) {
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
   * <p>This option will be ignored if {@link #TARGET_FRAME} is not set or is set to
   * {@link #DEFAULT_FRAME}.
   *
   * @see MediaMetadataRetriever#getFrameAtTime(long, int)
   */
  public static final Option<Integer> FRAME_OPTION = Option.disk(
      "com.bumptech.glide.load.resource.bitmap.VideoBitmapDecode.FrameOption",
      null /*defaultValue*/,
      new Option.CacheKeyUpdater<Integer>() {
        private final ByteBuffer buffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        @Override
        public void update(byte[] keyBytes, Integer value, MessageDigest messageDigest) {
          if (value == null) {
            return;
          }
          messageDigest.update(keyBytes);
          synchronized (buffer) {
            buffer.position(0);
            messageDigest.update(buffer.putInt(value).array());
          }
        }
      }
  );

  private static final MediaMetadataRetrieverFactory DEFAULT_FACTORY =
      new MediaMetadataRetrieverFactory();

  private final BitmapPool bitmapPool;
  private final MediaMetadataRetrieverFactory factory;

  public VideoBitmapDecoder(Context context) {
    this(Glide.get(context).getBitmapPool());
  }

  public VideoBitmapDecoder(BitmapPool bitmapPool) {
    this(bitmapPool, DEFAULT_FACTORY);
  }

  // Visible for testing.
  VideoBitmapDecoder(BitmapPool bitmapPool, MediaMetadataRetrieverFactory factory) {
    this.bitmapPool = bitmapPool;
    this.factory = factory;
  }

  @Override
  public boolean handles(ParcelFileDescriptor data, Options options) {
    MediaMetadataRetriever retriever = factory.build();
    try {
      retriever.setDataSource(data.getFileDescriptor());
      return true;
    } catch (RuntimeException e) {
      // Throws a generic runtime exception when given invalid data.
      return false;
    } finally {
      retriever.release();
    }
  }

  @Override
  public Resource<Bitmap> decode(ParcelFileDescriptor resource, int outWidth, int outHeight,
      Options options) throws IOException {
    long frameTimeMicros = options.get(TARGET_FRAME);
    if (frameTimeMicros < 0 && frameTimeMicros != DEFAULT_FRAME) {
      throw new IllegalArgumentException(
          "Requested frame must be non-negative, or DEFAULT_FRAME, given: " + frameTimeMicros);
    }
    Integer frameOption = options.get(FRAME_OPTION);

    final Bitmap result;
    MediaMetadataRetriever mediaMetadataRetriever = factory.build();
    try {
      mediaMetadataRetriever.setDataSource(resource.getFileDescriptor());
      if (frameTimeMicros == DEFAULT_FRAME) {
        result = mediaMetadataRetriever.getFrameAtTime();
      } else if (frameOption == null) {
        result = mediaMetadataRetriever.getFrameAtTime(frameTimeMicros);
      } else {
        result = mediaMetadataRetriever.getFrameAtTime(frameTimeMicros, frameOption);
      }
    } finally {
      mediaMetadataRetriever.release();
    }
    resource.close();
    return BitmapResource.obtain(result, bitmapPool);
  }

  // Visible for testing.
  static class MediaMetadataRetrieverFactory {
    public MediaMetadataRetriever build() {
      return new MediaMetadataRetriever();
    }
  }
}
