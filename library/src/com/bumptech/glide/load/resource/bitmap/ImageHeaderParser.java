package com.bumptech.glide.load.resource.bitmap;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.bumptech.glide.load.resource.bitmap.ImageHeaderParser.ImageType.GIF;
import static com.bumptech.glide.load.resource.bitmap.ImageHeaderParser.ImageType.JPEG;
import static com.bumptech.glide.load.resource.bitmap.ImageHeaderParser.ImageType.PNG;
import static com.bumptech.glide.load.resource.bitmap.ImageHeaderParser.ImageType.PNG_A;
import static com.bumptech.glide.load.resource.bitmap.ImageHeaderParser.ImageType.UNKNOWN;

/**
 * A class for parsing the exif orientation from an InputStream for an image. Handles jpegs and tiffs.
 */
public class ImageHeaderParser {
    private static final String TAG = "ImageHeaderParser";

    public static enum ImageType {
        /** GIF type */
        GIF(true),
        /** JPG type */
        JPEG(false),
        /** PNG type with alpha */
        PNG_A(true),
        /** PNG type without alpha */
        PNG(false),
        /** Unrecognized type */
        UNKNOWN(false);
        private final boolean hasAlpha;

        ImageType(boolean hasAlpha) {
            this.hasAlpha = hasAlpha;
        }

        public boolean hasAlpha() {
            return hasAlpha;
        }
    }

    private static final int GIF_HEADER = 0x474946;
    private static final int PNG_HEADER = 0x89504E47;
    private static final int EXIF_MAGIC_NUMBER = 0xFFD8;
    private static final int MOTOROLA_TIFF_MAGIC_NUMBER = 0x4D4D;  // "MM"
    private static final int INTEL_TIFF_MAGIC_NUMBER = 0x4949;     // "II"
    private static final String JPEG_EXIF_SEGMENT_PREAMBLE = "Exif\0\0";

    private static final int SEGMENT_SOS = 0xDA;
    private static final int MARKER_EOI = 0xD9;

    private static final int SEGMENT_START_ID = 0xFF;
    private static final int EXIF_SEGMENT_TYPE = 0xE1;

    private static final int ORIENTATION_TAG_TYPE = 0x0112;

    private static final int[] BYTES_PER_FORMAT = { 0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8 };

    private final StreamReader streamReader;

    public ImageHeaderParser(InputStream is) {
        streamReader = new StreamReader(is);
    }

    // 0xD0A3C68 -> <htm
    // 0xCAFEBABE -> <!DOCTYPE...
    public boolean hasAlpha() throws IOException {
        return getType().hasAlpha();
    }

    public ImageType getType() throws IOException {
        int firstByte = streamReader.getUInt8();

        if (firstByte == EXIF_MAGIC_NUMBER >> 8) { //JPEG
            return JPEG;
        }

        final int firstTwoBytes = firstByte << 8 & 0xFF00 | streamReader.getUInt8() & 0xFF;
        final int firstFourBytes = firstTwoBytes << 16 & 0xFFFF0000 | streamReader.getUInt16() & 0xFFFF;
        if (firstFourBytes == PNG_HEADER) { //PNG
            //see: http://stackoverflow.com/questions/2057923/how-to-check-a-png-for-grayscale-alpha-color-type
            streamReader.skip(25 - 4);
            int alpha = streamReader.getByte();
            // A RGB indexed PNG can also have transparency. Better safe than sorry!
            return alpha >= 3 ? PNG_A : PNG;
        }

        if (firstFourBytes >> 8 == GIF_HEADER) { //GIF from first 3 bytes
            return GIF;
        }

        return UNKNOWN;
    }

    /**
     * Parse the orientation from the image header. If it doesn't handle this image type (or this is not an image)
     * it will return a default value rather than throwing an exception.
     *
     * @return The exif orientation if present or -1 if the header couldn't be parsed or doesn't contain an orientation
     * @throws IOException
     */
    public int getOrientation() throws IOException {
        final int magicNumber = streamReader.getUInt16();

        if (!handles(magicNumber)) {
            return -1;
        } else {
            byte[] exifData = getExifSegment();
            if (exifData != null && exifData.length >= JPEG_EXIF_SEGMENT_PREAMBLE.length()
                    && new String(exifData, 0, JPEG_EXIF_SEGMENT_PREAMBLE.length())
                        .equalsIgnoreCase(JPEG_EXIF_SEGMENT_PREAMBLE)) {
                return parseExifSegment(new RandomAccessReader(exifData));
            } else {
                return -1;
            }
        }
    }

    private byte[] getExifSegment() throws IOException {
        short segmentId, segmentType;
        int segmentLength;
        while (true) {
            segmentId = streamReader.getUInt8();

            if (segmentId != SEGMENT_START_ID) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Unknown segmentId=" + segmentId);
                }
                return null;
            }

            segmentType = streamReader.getUInt8();

            if (segmentType == SEGMENT_SOS) {
                return null;
            } else if (segmentType == MARKER_EOI) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Found MARKER_EOI in exif segment");
                }
                return null;
            }

            segmentLength = streamReader.getUInt16() - 2; //segment length includes bytes for segment length

            if (segmentType != EXIF_SEGMENT_TYPE) {
                if (segmentLength != streamReader.skip(segmentLength)) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Unable to skip enough data for type=" + segmentType);
                    }
                    return null;
                }
            } else {
                byte[] segmentData = new byte[segmentLength];

                if (segmentLength != streamReader.read(segmentData)) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Unable to read segment data for type=" + segmentType + " length=" + segmentLength);
                    }
                    return null;
                } else {
                    return segmentData;
                }
            }
        }
    }

    private int parseExifSegment(RandomAccessReader segmentData) {

        final int headerOffsetSize = JPEG_EXIF_SEGMENT_PREAMBLE.length();

        short byteOrderIdentifier = segmentData.getInt16(headerOffsetSize);
        final ByteOrder byteOrder;
        if (byteOrderIdentifier == MOTOROLA_TIFF_MAGIC_NUMBER) { //
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

            if (tagType != ORIENTATION_TAG_TYPE) { //we only want orientation
                continue;
            }

            formatCode = segmentData.getInt16(tagOffset + 2);

            if (formatCode < 1 || formatCode > 12) { //12 is max format code
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
                Log.d(TAG, "Got tagIndex=" + i + " tagType=" + tagType + " formatCode =" + formatCode
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
        return ifdOffset + 2 + (12 * tagIndex);
    }

    private boolean handles(int imageMagicNumber) {
        return (imageMagicNumber & EXIF_MAGIC_NUMBER) == EXIF_MAGIC_NUMBER ||
                imageMagicNumber == MOTOROLA_TIFF_MAGIC_NUMBER ||
                imageMagicNumber == INTEL_TIFF_MAGIC_NUMBER;
    }

    private static class RandomAccessReader {
        private final ByteBuffer data;

        public RandomAccessReader(byte[] data) {
            this.data = ByteBuffer.wrap(data);
            this.data.order(ByteOrder.BIG_ENDIAN);
        }

        public void order(ByteOrder byteOrder) {
            this.data.order(byteOrder);
        }

        public int length() {
            return data.array().length;
        }

        public int getInt32(int offset) {
            return data.getInt(offset);
        }

        public short getInt16(int offset) {
            return data.getShort(offset);
        }
    }

    private static class StreamReader {
        private final InputStream is;
        //motorola / big endian byte order

        public StreamReader(InputStream is) {
            this.is = is;
        }

        public int getUInt16() throws IOException {
            return  (is.read() << 8 & 0xFF00) | (is.read() & 0xFF);
        }

        public short getUInt8() throws IOException {
            return (short) (is.read() & 0xFF);
        }

        public long skip(long total) throws IOException {
            return is.skip(total);
        }

        public int read(byte[] buffer) throws IOException {
            return is.read(buffer);
        }

        public int getByte() throws IOException {
            return is.read();
        }
    }
}

