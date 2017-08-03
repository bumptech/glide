package com.bumptech.glide.load.resource.bitmap;

import static com.bumptech.glide.load.ImageHeaderParser.ImageType.GIF;
import static com.bumptech.glide.load.ImageHeaderParser.ImageType.JPEG;
import static com.bumptech.glide.load.ImageHeaderParser.ImageType.PNG;
import static com.bumptech.glide.load.ImageHeaderParser.ImageType.PNG_A;
import static com.bumptech.glide.load.ImageHeaderParser.ImageType.UNKNOWN;

import android.util.Log;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.util.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * A class for parsing the exif orientation and other data from an image header.
 */
public final class DefaultImageHeaderParser implements ImageHeaderParser {
  // Due to https://code.google.com/p/android/issues/detail?id=97751.
  // TAG needs to be under 23 chars, so "Default" > "Dflt".
  private static final String TAG = "DfltImageHeaderParser";

  private static final int GIF_HEADER = 0x474946;
  private static final int PNG_HEADER = 0x89504E47;
  static final int EXIF_MAGIC_NUMBER = 0xFFD8;
  // "MM".
  private static final int MOTOROLA_TIFF_MAGIC_NUMBER = 0x4D4D;
  // "II".
  private static final int INTEL_TIFF_MAGIC_NUMBER = 0x4949;
  static final String JPEG_EXIF_SEGMENT_PREAMBLE = "Exif\0\0";
  static final byte[] JPEG_EXIF_SEGMENT_PREAMBLE_BYTES =
      JPEG_EXIF_SEGMENT_PREAMBLE.getBytes(Charset.forName("UTF-8"));
  private static final int SEGMENT_SOS = 0xDA;
  private static final int MARKER_EOI = 0xD9;
  static final int SEGMENT_START_ID = 0xFF;
  static final int EXIF_SEGMENT_TYPE = 0xE1;
  private static final int ORIENTATION_TAG_TYPE = 0x0112;
  private static final int[] BYTES_PER_FORMAT = { 0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8 };
  // WebP-related
  // "RIFF"
  private static final int RIFF_HEADER = 0x52494646;
  // "WEBP"
  private static final int WEBP_HEADER = 0x57454250;
  // "VP8" null.
  private static final int VP8_HEADER = 0x56503800;
  private static final int VP8_HEADER_MASK = 0xFFFFFF00;
  private static final int VP8_HEADER_TYPE_MASK = 0x000000FF;
  // 'X'
  private static final int VP8_HEADER_TYPE_EXTENDED = 0x00000058;
  // 'L'
  private static final int VP8_HEADER_TYPE_LOSSLESS = 0x0000004C;
  private static final int WEBP_EXTENDED_ALPHA_FLAG = 1 << 4;
  private static final int WEBP_LOSSLESS_ALPHA_FLAG = 1 << 3;

  @Override
  public ImageType getType(InputStream is) throws IOException {
    return getType(new StreamReader(Preconditions.checkNotNull(is)));
  }

  @Override
  public ImageType getType(ByteBuffer byteBuffer) throws IOException {
    return getType(new ByteBufferReader(Preconditions.checkNotNull(byteBuffer)));
  }

  @Override
  public int getOrientation(InputStream is, ArrayPool byteArrayPool) throws IOException {
    return getOrientation(new StreamReader(Preconditions.checkNotNull(is)),
        Preconditions.checkNotNull(byteArrayPool));
  }

  @Override
  public int getOrientation(ByteBuffer byteBuffer, ArrayPool byteArrayPool) throws IOException {
    return getOrientation(new ByteBufferReader(Preconditions.checkNotNull(byteBuffer)),
        Preconditions.checkNotNull(byteArrayPool));
  }

  private ImageType getType(Reader reader) throws IOException {
    int firstTwoBytes = reader.getUInt16();

    // JPEG.
    if (firstTwoBytes == EXIF_MAGIC_NUMBER) {
      return JPEG;
    }

    final int firstFourBytes = firstTwoBytes << 16 & 0xFFFF0000 | reader.getUInt16() & 0xFFFF;
    // PNG.
    if (firstFourBytes == PNG_HEADER) {
      // See: http://stackoverflow.com/questions/2057923/how-to-check-a-png-for-grayscale-alpha
      // -color-type
      reader.skip(25 - 4);
      int alpha = reader.getByte();
      // A RGB indexed PNG can also have transparency. Better safe than sorry!
      return alpha >= 3 ? PNG_A : PNG;
    }

    // GIF from first 3 bytes.
    if (firstFourBytes >> 8 == GIF_HEADER) {
      return GIF;
    }

    // WebP (reads up to 21 bytes). See https://developers.google.com/speed/webp/docs/riff_container
    // for details.
    if (firstFourBytes != RIFF_HEADER) {
      return UNKNOWN;
    }
    // Bytes 4 - 7 contain length information. Skip these.
    reader.skip(4);
    final int thirdFourBytes = reader.getUInt16() << 16 & 0xFFFF0000 | reader.getUInt16() & 0xFFFF;
    if (thirdFourBytes != WEBP_HEADER) {
      return UNKNOWN;
    }
    final int fourthFourBytes = reader.getUInt16() << 16 & 0xFFFF0000 | reader.getUInt16() & 0xFFFF;
    if ((fourthFourBytes & VP8_HEADER_MASK) != VP8_HEADER) {
      return UNKNOWN;
    }
    if ((fourthFourBytes & VP8_HEADER_TYPE_MASK) == VP8_HEADER_TYPE_EXTENDED) {
      // Skip some more length bytes and check for transparency/alpha flag.
      reader.skip(4);
      return (reader.getByte() & WEBP_EXTENDED_ALPHA_FLAG) != 0 ? ImageType.WEBP_A : ImageType.WEBP;
    }
    if ((fourthFourBytes & VP8_HEADER_TYPE_MASK) == VP8_HEADER_TYPE_LOSSLESS) {
      // See chromium.googlesource.com/webm/libwebp/+/master/doc/webp-lossless-bitstream-spec.txt
      // for more info.
      reader.skip(4);
      return (reader.getByte() & WEBP_LOSSLESS_ALPHA_FLAG) != 0 ? ImageType.WEBP_A : ImageType.WEBP;
    }
    return ImageType.WEBP;
  }

  /**
   * Parse the orientation from the image header. If it doesn't handle this image type (or this is
   * not an image) it will return a default value rather than throwing an exception.
   *
   * @return The exif orientation if present or -1 if the header couldn't be parsed or doesn't
   * contain an orientation
   * @throws IOException
   */
  private int getOrientation(Reader reader, ArrayPool byteArrayPool) throws IOException {
    final int magicNumber = reader.getUInt16();

    if (!handles(magicNumber)) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Parser doesn't handle magic number: " + magicNumber);
      }
      return UNKNOWN_ORIENTATION;
    } else {
      int exifSegmentLength = moveToExifSegmentAndGetLength(reader);
      if (exifSegmentLength == -1) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Failed to parse exif segment length, or exif segment not found");
        }
        return UNKNOWN_ORIENTATION;
      }

      byte[] exifData = byteArrayPool.get(exifSegmentLength, byte[].class);
      try {
        return parseExifSegment(reader, exifData, exifSegmentLength);
      } finally {
        byteArrayPool.put(exifData, byte[].class);
      }
    }
  }

  private int parseExifSegment(Reader reader, byte[] tempArray, int exifSegmentLength)
      throws IOException {
    int read = reader.read(tempArray, exifSegmentLength);
    if (read != exifSegmentLength) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Unable to read exif segment data"
            + ", length: " + exifSegmentLength
            + ", actually read: " + read);
      }
      return UNKNOWN_ORIENTATION;
    }

    boolean hasJpegExifPreamble = hasJpegExifPreamble(tempArray, exifSegmentLength);
    if (hasJpegExifPreamble) {
      return parseExifSegment(new RandomAccessReader(tempArray, exifSegmentLength));
    } else {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Missing jpeg exif preamble");
      }
      return UNKNOWN_ORIENTATION;
    }
  }

  private boolean hasJpegExifPreamble(byte[] exifData, int exifSegmentLength) {
    boolean result =
        exifData != null && exifSegmentLength > JPEG_EXIF_SEGMENT_PREAMBLE_BYTES.length;
    if (result) {
      for (int i = 0; i < JPEG_EXIF_SEGMENT_PREAMBLE_BYTES.length; i++) {
        if (exifData[i] != JPEG_EXIF_SEGMENT_PREAMBLE_BYTES[i]) {
          result = false;
          break;
        }
      }
    }
    return result;
  }

  /**
   * Moves reader to the start of the exif segment and returns the length of the exif segment or
   * {@code -1} if no exif segment is found.
   */
  private int moveToExifSegmentAndGetLength(Reader reader) throws IOException {
    short segmentId, segmentType;
    int segmentLength;
    while (true) {
      segmentId = reader.getUInt8();
      if (segmentId != SEGMENT_START_ID) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Unknown segmentId=" + segmentId);
        }
        return -1;
      }

      segmentType = reader.getUInt8();

      if (segmentType == SEGMENT_SOS) {
        return -1;
      } else if (segmentType == MARKER_EOI) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Found MARKER_EOI in exif segment");
        }
        return -1;
      }

      // Segment length includes bytes for segment length.
      segmentLength = reader.getUInt16() - 2;

      if (segmentType != EXIF_SEGMENT_TYPE) {
        long skipped = reader.skip(segmentLength);
        if (skipped != segmentLength) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Unable to skip enough data"
                    + ", type: " + segmentType
                    + ", wanted to skip: " + segmentLength
                    + ", but actually skipped: " + skipped);
            }
            return -1;
        }
      } else {
        return segmentLength;
      }
    }
  }

  private static int parseExifSegment(RandomAccessReader segmentData) {
    final int headerOffsetSize = JPEG_EXIF_SEGMENT_PREAMBLE.length();

    short byteOrderIdentifier = segmentData.getInt16(headerOffsetSize);
    final ByteOrder byteOrder;
    if (byteOrderIdentifier == MOTOROLA_TIFF_MAGIC_NUMBER) {
      byteOrder = ByteOrder.BIG_ENDIAN;
    } else if (byteOrderIdentifier == INTEL_TIFF_MAGIC_NUMBER) {
      byteOrder = ByteOrder.LITTLE_ENDIAN;
    } else {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Unknown endianness = " + byteOrderIdentifier);
      }
      byteOrder = ByteOrder.BIG_ENDIAN;
    }

    segmentData.order(byteOrder);

    int firstIfdOffset = segmentData.getInt32(headerOffsetSize + 4) + headerOffsetSize;
    int tagCount = segmentData.getInt16(firstIfdOffset);

    int tagOffset, tagType, formatCode, componentCount;
    for (int i = 0; i < tagCount; i++) {
      tagOffset = calcTagOffset(firstIfdOffset, i);
      tagType = segmentData.getInt16(tagOffset);

      // We only want orientation.
      if (tagType != ORIENTATION_TAG_TYPE) {
        continue;
      }

      formatCode = segmentData.getInt16(tagOffset + 2);

      // 12 is max format code.
      if (formatCode < 1 || formatCode > 12) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Got invalid format code = " + formatCode);
        }
        continue;
      }

      componentCount = segmentData.getInt32(tagOffset + 4);

      if (componentCount < 0) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Negative tiff component count");
        }
        continue;
      }

      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Got tagIndex=" + i + " tagType=" + tagType + " formatCode=" + formatCode
            + " componentCount=" + componentCount);
      }

      final int byteCount = componentCount + BYTES_PER_FORMAT[formatCode];

      if (byteCount > 4) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Got byte count > 4, not orientation, continuing, formatCode=" + formatCode);
        }
        continue;
      }

      final int tagValueOffset = tagOffset + 8;

      if (tagValueOffset < 0 || tagValueOffset > segmentData.length()) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Illegal tagValueOffset=" + tagValueOffset + " tagType=" + tagType);
        }
        continue;
      }

      if (byteCount < 0 || tagValueOffset + byteCount > segmentData.length()) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Illegal number of bytes for TI tag data tagType=" + tagType);
        }
        continue;
      }

      //assume componentCount == 1 && fmtCode == 3
      return segmentData.getInt16(tagValueOffset);
    }

    return -1;
  }

  private static int calcTagOffset(int ifdOffset, int tagIndex) {
    return ifdOffset + 2 + 12 * tagIndex;
  }

  private static boolean handles(int imageMagicNumber) {
    return (imageMagicNumber & EXIF_MAGIC_NUMBER) == EXIF_MAGIC_NUMBER
        || imageMagicNumber == MOTOROLA_TIFF_MAGIC_NUMBER
        || imageMagicNumber == INTEL_TIFF_MAGIC_NUMBER;
  }

  private static final class RandomAccessReader {
    private final ByteBuffer data;

    RandomAccessReader(byte[] data, int length) {
      this.data = (ByteBuffer) ByteBuffer.wrap(data)
          .order(ByteOrder.BIG_ENDIAN)
          .limit(length);
    }

    void order(ByteOrder byteOrder) {
      this.data.order(byteOrder);
    }

    int length() {
      return data.remaining();
    }

    int getInt32(int offset) {
      return isAvailable(offset, 4) ? data.getInt(offset) : -1;
    }

    short getInt16(int offset) {
      return isAvailable(offset, 2) ? data.getShort(offset) : -1;
    }

    private boolean isAvailable(int offset, int byteSize) {
      return data.remaining() - offset >= byteSize;
    }
  }

  private interface Reader {
    int getUInt16() throws IOException;
    short getUInt8() throws IOException;
    long skip(long total) throws IOException;
    int read(byte[] buffer, int byteCount) throws IOException;
    int getByte() throws IOException;
  }

  private static final class ByteBufferReader implements Reader {

    private final ByteBuffer byteBuffer;

    ByteBufferReader(ByteBuffer byteBuffer) {
      this.byteBuffer = byteBuffer;
      byteBuffer.order(ByteOrder.BIG_ENDIAN);
    }

    @Override
    public int getUInt16() throws IOException {
      return (getByte() << 8 & 0xFF00) | (getByte() & 0xFF);
    }

    @Override
    public short getUInt8() throws IOException {
      return (short) (getByte() & 0xFF);
    }

    @Override
    public long skip(long total) throws IOException {
      int toSkip = (int) Math.min(byteBuffer.remaining(), total);
      byteBuffer.position(byteBuffer.position() + toSkip);
      return toSkip;
    }

    @Override
    public int read(byte[] buffer, int byteCount) throws IOException {
      int toRead = Math.min(byteCount, byteBuffer.remaining());
      if (toRead == 0) {
        return -1;
      }
      byteBuffer.get(buffer, 0 /*dstOffset*/, toRead);
      return toRead;
    }

    @Override
    public int getByte() throws IOException {
      if (byteBuffer.remaining() < 1) {
        return -1;
      }
      return byteBuffer.get();
    }
  }

  private static final class StreamReader implements Reader {
    private final InputStream is;
    // Motorola / big endian byte order.
    StreamReader(InputStream is) {
      this.is = is;
    }

    @Override
    public int getUInt16() throws IOException {
      return (is.read() << 8 & 0xFF00) | (is.read() & 0xFF);
    }

    @Override
    public short getUInt8() throws IOException {
      return (short) (is.read() & 0xFF);
    }

    @Override
    public long skip(long total) throws IOException {
      if (total < 0) {
        return 0;
      }

      long toSkip = total;
      while (toSkip > 0) {
        long skipped = is.skip(toSkip);
        if (skipped > 0) {
          toSkip -= skipped;
        } else {
          // Skip has no specific contract as to what happens when you reach the end of
          // the stream. To differentiate between temporarily not having more data and
          // having finished the stream, we read a single byte when we fail to skip any
          // amount of data.
          int testEofByte = is.read();
          if (testEofByte == -1) {
            break;
          } else {
            toSkip--;
          }
        }
      }
      return total - toSkip;
    }

    @Override
    public int read(byte[] buffer, int byteCount) throws IOException {
      int toRead = byteCount;
      int read;
      while (toRead > 0 && ((read = is.read(buffer, byteCount - toRead, toRead)) != -1)) {
        toRead -= read;
      }
      return byteCount - toRead;
    }

    @Override
    public int getByte() throws IOException {
      return is.read();
    }
  }
}
