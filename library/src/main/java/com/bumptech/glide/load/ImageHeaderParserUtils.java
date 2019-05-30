package com.bumptech.glide.load;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.ImageHeaderParser.ImageType;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.resource.bitmap.RecyclableBufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

/** Utilities for the ImageHeaderParser. */
public final class ImageHeaderParserUtils {
  // 5MB. This is the max image header size we can handle, we preallocate a much smaller buffer but
  // will resize up to this amount if necessary.
  private static final int MARK_POSITION = 5 * 1024 * 1024;

  private ImageHeaderParserUtils() {}

  /** Returns the ImageType for the given InputStream. */
  @NonNull
  public static ImageType getType(
      @NonNull List<ImageHeaderParser> parsers,
      @Nullable InputStream is,
      @NonNull ArrayPool byteArrayPool)
      throws IOException {
    if (is == null) {
      return ImageType.UNKNOWN;
    }

    if (!is.markSupported()) {
      is = new RecyclableBufferedInputStream(is, byteArrayPool);
    }

    is.mark(MARK_POSITION);
    //noinspection ForLoopReplaceableByForEach to improve perf
    for (int i = 0, size = parsers.size(); i < size; i++) {
      ImageHeaderParser parser = parsers.get(i);
      try {
        ImageType type = parser.getType(is);
        if (type != ImageType.UNKNOWN) {
          return type;
        }
      } finally {
        is.reset();
      }
    }

    return ImageType.UNKNOWN;
  }

  /** Returns the ImageType for the given ByteBuffer. */
  @NonNull
  public static ImageType getType(
      @NonNull List<ImageHeaderParser> parsers, @Nullable ByteBuffer buffer) throws IOException {
    if (buffer == null) {
      return ImageType.UNKNOWN;
    }

    //noinspection ForLoopReplaceableByForEach to improve perf
    for (int i = 0, size = parsers.size(); i < size; i++) {
      ImageHeaderParser parser = parsers.get(i);
      ImageType type = parser.getType(buffer);
      if (type != ImageType.UNKNOWN) {
        return type;
      }
    }

    return ImageType.UNKNOWN;
  }

  /** Returns the orientation for the given InputStream. */
  public static int getOrientation(
      @NonNull List<ImageHeaderParser> parsers,
      @Nullable InputStream is,
      @NonNull ArrayPool byteArrayPool)
      throws IOException {
    if (is == null) {
      return ImageHeaderParser.UNKNOWN_ORIENTATION;
    }

    if (!is.markSupported()) {
      is = new RecyclableBufferedInputStream(is, byteArrayPool);
    }

    is.mark(MARK_POSITION);
    //noinspection ForLoopReplaceableByForEach to improve perf
    for (int i = 0, size = parsers.size(); i < size; i++) {
      ImageHeaderParser parser = parsers.get(i);
      try {
        int orientation = parser.getOrientation(is, byteArrayPool);
        if (orientation != ImageHeaderParser.UNKNOWN_ORIENTATION) {
          return orientation;
        }
      } finally {
        is.reset();
      }
    }

    return ImageHeaderParser.UNKNOWN_ORIENTATION;
  }
}
