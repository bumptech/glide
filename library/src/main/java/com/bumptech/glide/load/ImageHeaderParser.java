package com.bumptech.glide.load;

import androidx.annotation.NonNull;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/** Interface for the ImageHeaderParser. */
public interface ImageHeaderParser {
  /**
   * A constant indicating we were unable to parse the orientation from the image either because no
   * exif segment containing orientation data existed, or because of an I/O error attempting to read
   * the exif segment.
   */
  int UNKNOWN_ORIENTATION = -1;

  /**
   * The format of the image data including whether or not the image may include transparent pixels.
   */
  enum ImageType {
    GIF(true),
    JPEG(false),
    RAW(false),
    /** PNG type with alpha. */
    PNG_A(true),
    /** PNG type without alpha. */
    PNG(false),
    /** WebP type with alpha. */
    WEBP_A(true),
    /** WebP type without alpha. */
    WEBP(false),
    /** All animated webps. */
    ANIMATED_WEBP(true),
    /** Avif type (may contain alpha). */
    AVIF(true),
    /** Animated Avif type (may contain alpha). */
    ANIMATED_AVIF(true),
    /** Unrecognized type. */
    UNKNOWN(false);

    private final boolean hasAlpha;

    ImageType(boolean hasAlpha) {
      this.hasAlpha = hasAlpha;
    }

    public boolean hasAlpha() {
      return hasAlpha;
    }

    public boolean isWebp() {
      switch (this) {
        case WEBP:
        case WEBP_A:
        case ANIMATED_WEBP:
          return true;
        default:
          return false;
      }
    }
  }

  @NonNull
  ImageType getType(@NonNull InputStream is) throws IOException;

  @NonNull
  ImageType getType(@NonNull ByteBuffer byteBuffer) throws IOException;

  /**
   * Parse the orientation from the image header. If it doesn't handle this image type (or this is
   * not an image) it will return a default value rather than throwing an exception.
   *
   * @return The exif orientation if present or -1 if the header couldn't be parsed or doesn't
   *     contain an orientation
   */
  int getOrientation(@NonNull InputStream is, @NonNull ArrayPool byteArrayPool) throws IOException;

  int getOrientation(@NonNull ByteBuffer byteBuffer, @NonNull ArrayPool byteArrayPool)
      throws IOException;
}
