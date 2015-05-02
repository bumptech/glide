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
   * A long indicating the target frame we should provide to
   * {@link android.media.MediaMetadataRetriever#getFrameAtTime(long)} when extracting a video
   * frame.
   */
  public static final Option<Long> TARGET_FRAME = Option.disk(
      "com.bumptech.glide.load.resource.bitmap.VideoBitmapDecode.TargetFrame", DEFAULT_FRAME,
      new Option.CacheKeyUpdater<Long>() {
        private final ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);
        @Override
        public void update(byte[] keyBytes, Long value, MessageDigest messageDigest) {
          messageDigest.update(keyBytes);
          synchronized (buffer) {
            buffer.rewind();
            messageDigest.update(buffer.putLong(value).array());
          }
        }
      });

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
    long frame = options.get(TARGET_FRAME);
    if (frame < 0 && frame != DEFAULT_FRAME) {
      throw new IllegalArgumentException(
          "Requested frame must be non-negative, or DEFAULT_FRAME, given: " + frame);
    }

    MediaMetadataRetriever mediaMetadataRetriever = factory.build();
    mediaMetadataRetriever.setDataSource(resource.getFileDescriptor());
    final Bitmap result;
    if (frame == DEFAULT_FRAME) {
      result = mediaMetadataRetriever.getFrameAtTime();
    } else {
      result = mediaMetadataRetriever.getFrameAtTime(frame);
    }
    mediaMetadataRetriever.release();
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
