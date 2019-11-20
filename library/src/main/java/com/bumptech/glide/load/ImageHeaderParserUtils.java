package com.bumptech.glide.load;

import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.load.ImageHeaderParser.ImageType;
import com.bumptech.glide.load.data.ParcelFileDescriptorRewinder;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.resource.bitmap.RecyclableBufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

/** Utilities for the ImageHeaderParser. */
public final class ImageHeaderParserUtils {
  // 5MB. This is the max image header size we can handle, we preallocate a much smaller buffer but
  // will resize up to this amount if necessary.
  private static final int MARK_READ_LIMIT = 5 * 1024 * 1024;

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

    is.mark(MARK_READ_LIMIT);
    final InputStream finalIs = is;
    return getTypeInternal(
        parsers,
        new TypeReader() {
          @Override
          public ImageType getType(ImageHeaderParser parser) throws IOException {
            try {
              return parser.getType(finalIs);
            } finally {
              finalIs.reset();
            }
          }
        });
  }

  /** Returns the ImageType for the given ByteBuffer. */
  @NonNull
  public static ImageType getType(
      @NonNull List<ImageHeaderParser> parsers, @Nullable final ByteBuffer buffer)
      throws IOException {
    if (buffer == null) {
      return ImageType.UNKNOWN;
    }

    return getTypeInternal(
        parsers,
        new TypeReader() {
          @Override
          public ImageType getType(ImageHeaderParser parser) throws IOException {
            return parser.getType(buffer);
          }
        });
  }

  @NonNull
  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  public static ImageType getType(
      @NonNull List<ImageHeaderParser> parsers,
      @NonNull final ParcelFileDescriptorRewinder parcelFileDescriptorRewinder,
      @NonNull final ArrayPool byteArrayPool)
      throws IOException {
    return getTypeInternal(
        parsers,
        new TypeReader() {
          @Override
          public ImageType getType(ImageHeaderParser parser) throws IOException {
            // Wrap the FileInputStream into a RecyclableBufferedInputStream to optimize I/O
            // performance
            InputStream is = null;
            try {
              is =
                  new RecyclableBufferedInputStream(
                      new FileInputStream(
                          parcelFileDescriptorRewinder.rewindAndGet().getFileDescriptor()),
                      byteArrayPool);
              return parser.getType(is);
            } finally {
              try {
                if (is != null) {
                  is.close();
                }
              } catch (IOException e) {
                // Ignored.
              }
              parcelFileDescriptorRewinder.rewindAndGet();
            }
          }
        });
  }

  @NonNull
  private static ImageType getTypeInternal(
      @NonNull List<ImageHeaderParser> parsers, TypeReader reader) throws IOException {
    //noinspection ForLoopReplaceableByForEach to improve perf
    for (int i = 0, size = parsers.size(); i < size; i++) {
      ImageHeaderParser parser = parsers.get(i);
      ImageType type = reader.getType(parser);
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
      @NonNull final ArrayPool byteArrayPool)
      throws IOException {
    if (is == null) {
      return ImageHeaderParser.UNKNOWN_ORIENTATION;
    }

    if (!is.markSupported()) {
      is = new RecyclableBufferedInputStream(is, byteArrayPool);
    }

    is.mark(MARK_READ_LIMIT);
    final InputStream finalIs = is;
    return getOrientationInternal(
        parsers,
        new OrientationReader() {
          @Override
          public int getOrientation(ImageHeaderParser parser) throws IOException {
            try {
              return parser.getOrientation(finalIs, byteArrayPool);
            } finally {
              finalIs.reset();
            }
          }
        });
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  public static int getOrientation(
      @NonNull List<ImageHeaderParser> parsers,
      @NonNull final ParcelFileDescriptorRewinder parcelFileDescriptorRewinder,
      @NonNull final ArrayPool byteArrayPool)
      throws IOException {
    return getOrientationInternal(
        parsers,
        new OrientationReader() {
          @Override
          public int getOrientation(ImageHeaderParser parser) throws IOException {
            // Wrap the FileInputStream into a RecyclableBufferedInputStream to optimize I/O
            // performance
            InputStream is = null;
            try {
              is =
                  new RecyclableBufferedInputStream(
                      new FileInputStream(
                          parcelFileDescriptorRewinder.rewindAndGet().getFileDescriptor()),
                      byteArrayPool);
              return parser.getOrientation(is, byteArrayPool);
            } finally {
              try {
                if (is != null) {
                  is.close();
                }
              } catch (IOException e) {
                // Ignored.
              }
              parcelFileDescriptorRewinder.rewindAndGet();
            }
          }
        });
  }

  private static int getOrientationInternal(
      @NonNull List<ImageHeaderParser> parsers, OrientationReader reader) throws IOException {
    //noinspection ForLoopReplaceableByForEach to improve perf
    for (int i = 0, size = parsers.size(); i < size; i++) {
      ImageHeaderParser parser = parsers.get(i);
      int orientation = reader.getOrientation(parser);
      if (orientation != ImageHeaderParser.UNKNOWN_ORIENTATION) {
        return orientation;
      }
    }

    return ImageHeaderParser.UNKNOWN_ORIENTATION;
  }

  private interface TypeReader {
    ImageType getType(ImageHeaderParser parser) throws IOException;
  }

  private interface OrientationReader {
    int getOrientation(ImageHeaderParser parser) throws IOException;
  }
}
