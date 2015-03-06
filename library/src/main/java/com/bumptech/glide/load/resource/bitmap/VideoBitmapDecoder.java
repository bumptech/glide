package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.io.IOException;
import java.util.Map;

/**
 * An {@link com.bumptech.glide.load.ResourceDecoder} that can decode a thumbnail frame
 * {@link android.graphics.Bitmap} from a {@link android.os.ParcelFileDescriptor} containing a
 * video.
 *
 * @see android.media.MediaMetadataRetriever
 */
public class VideoBitmapDecoder implements ResourceDecoder<ParcelFileDescriptor, Bitmap> {
  /**
   * A key for a long indicating the target frame we should provide to
   * {@link android.media.MediaMetadataRetriever#getFrameAtTime(long)} when extracting a video
   * frame.
   */
  public static final String KEY_TARGET_FRAME =
      "com.bumtpech.glide.load.resource.bitmap.VideoBitmapDecode.TargetFrame";
  /**
   * A constant indicating we should use whatever frame we consider best, frequently not the first
   * frame.
   */
  public static final int DEFAULT_FRAME = -1;

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
  public boolean handles(ParcelFileDescriptor data, Map<String, Object> options) {
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
      Map<String, Object> options) throws IOException {
    int frame = options.containsKey(KEY_TARGET_FRAME)
        ? (Integer) options.get(KEY_TARGET_FRAME) : DEFAULT_FRAME;
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
