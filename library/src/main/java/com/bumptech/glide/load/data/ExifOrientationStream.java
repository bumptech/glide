package com.bumptech.glide.load.data;

import androidx.annotation.NonNull;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Adds an exif segment with an orientation attribute to a wrapped {@link InputStream} containing
 * image data.
 *
 * <p>This class assumes that the wrapped stream contains an image format that can contain exif
 * information and performs no verification.
 */
public final class ExifOrientationStream extends FilterInputStream {
  /** Allow two bytes for the file format. */
  private static final int SEGMENT_START_POSITION = 2;

  private static final byte[] EXIF_SEGMENT =
      new byte[] {
        /* segment start id. */
        (byte) 0xFF,
        /* segment type. */
        (byte) 0xE1,
        /* segmentLength. */
        0x00,
        (byte) 0x1C,
        /* exif identifier. */
        0x45,
        0x78,
        0x69,
        0x66,
        0x00,
        0x00,
        /* motorola byte order (big endian). */
        (byte) 0x4D,
        (byte) 0x4D,
        /* filler? */
        0x00,
        0x00,
        /* first id offset. */
        0x00,
        0x00,
        0x00,
        0x08,
        /* tagCount. */
        0x00,
        0x01,
        /* exif tag type. */
        0x01,
        0x12,
        /* 2 byte format. */
        0x00,
        0x02,
        /* component count. */
        0x00,
        0x00,
        0x00,
        0x01,
        /* 2 byte orientation value, the first byte of which is always 0. */
        0x00,
      };
  private static final int SEGMENT_LENGTH = EXIF_SEGMENT.length;
  private static final int ORIENTATION_POSITION = SEGMENT_LENGTH + SEGMENT_START_POSITION;
  private final byte orientation;
  private int position;

  public ExifOrientationStream(InputStream in, int orientation) {
    super(in);
    if (orientation < -1 || orientation > 8) {
      throw new IllegalArgumentException("Cannot add invalid orientation: " + orientation);
    }
    this.orientation = (byte) orientation;
  }

  @Override
  public boolean markSupported() {
    return false;
  }

  // No need for synchronized since all we do is throw.
  @SuppressWarnings("UnsynchronizedOverridesSynchronized")
  @Override
  public void mark(int readLimit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int read() throws IOException {
    final int result;
    if (position < SEGMENT_START_POSITION || position > ORIENTATION_POSITION) {
      result = super.read();
    } else if (position == ORIENTATION_POSITION) {
      result = orientation;
    } else {
      result = EXIF_SEGMENT[position - SEGMENT_START_POSITION] & 0xFF;
    }
    if (result != -1) {
      position++;
    }
    return result;
  }

  @Override
  public int read(@NonNull byte[] buffer, int byteOffset, int byteCount) throws IOException {
    int read;
    if (position > ORIENTATION_POSITION) {
      read = super.read(buffer, byteOffset, byteCount);
    } else if (position == ORIENTATION_POSITION) {
      buffer[byteOffset] = orientation;
      read = 1;
    } else if (position < SEGMENT_START_POSITION) {
      read = super.read(buffer, byteOffset, SEGMENT_START_POSITION - position);
    } else {
      read = Math.min(ORIENTATION_POSITION - position, byteCount);
      System.arraycopy(EXIF_SEGMENT, position - SEGMENT_START_POSITION, buffer, byteOffset, read);
    }
    if (read > 0) {
      position += read;
    }
    return read;
  }

  @Override
  public long skip(long byteCount) throws IOException {
    long skipped = super.skip(byteCount);
    if (skipped > 0) {
      // See https://errorprone.info/bugpattern/NarrowingCompoundAssignment.
      position = (int) (position + skipped);
    }
    return skipped;
  }

  // No need for synchronized since all we do is throw.
  @SuppressWarnings("UnsynchronizedOverridesSynchronized")
  @Override
  public void reset() throws IOException {
    throw new UnsupportedOperationException();
  }
}
