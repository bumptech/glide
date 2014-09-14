package com.bumptech.glide.gifdecoder.test;

import java.nio.ByteBuffer;

/**
 * Utils for writing the bytes of various parts of GIFs to byte buffers.
 */
public class GifBytesTestUtil {
    // Length in bytes.
    public static final int HEADER_LENGTH = 13;
    // Length in bytes.
    public static final int IMAGE_DESCRIPTOR_LENGTH = 10;

    public static void writeFakeImageData(ByteBuffer out, int lzwMinCodeSize) {
        // 1 for lzwMinCodeSize, 1 for length, 1 for min content, 1 for block terminator.
        verifyRemaining(out, 4);
        verifyShortValues(lzwMinCodeSize);

        out.put((byte) lzwMinCodeSize);
        // Block length.
        out.put((byte) 0x01);
        // Block content.
        out.put((byte) 0x01);
        // End of block.
        out.put((byte) 0x00);
    }

    public static void writeImageDescriptor(ByteBuffer out, int imageLeft, int imageTop, int imageWidth,
            int imageHeight) {
        verifyRemaining(out, IMAGE_DESCRIPTOR_LENGTH);
        verifyShortValues(imageLeft, imageTop, imageWidth, imageHeight);

        // Image separator
        out.put((byte) 0x2C);

        out.putShort((short) imageLeft).putShort((short) imageTop).putShort((short) imageWidth)
                .putShort((short) imageHeight);
    }

    public static void writeHeaderAndLsd(ByteBuffer out, int width, int height, boolean hasGct, int gctSize) {
        verifyRemaining(out, HEADER_LENGTH);
        verifyShortValues(width, height);

        // GIF
        out.put((byte) 0x47).put((byte) 0x49).put((byte) 0x46);
        // Version - 89a.
        out.put((byte) 0x38).put((byte) 0x39).put((byte) 0x61);

        /** LSD (Logical Screen Descriptor) **/
          // Width.
        out.putShort((short) width);
        // Height.
        out.putShort((short) height);
        // Packed GCT (Global Color Table) flag + color resolution + sort flag + size of GCT.
        // GCT flag (false) - most significant bit.
        byte gctFlag = (byte) ((hasGct ? 1 : 0) << 7);
        // Color resolution - next three bits.
        byte colorResolution = 1 << 5;
        // Sort flag - next bit;
        byte sortFlag = 0 << 4;
        // exponent of size of color table, size = 2^(1 + exponent) - least significant 3 bits.
        byte size = (byte) gctSize;

        byte packed = (byte) (gctFlag | colorResolution | sortFlag | size);
        out.put(packed);

        // Background color index.
        out.put((byte) 0);

        // Pixel aspect ratio.
        out.put((byte) 0);
    }

    private static void verifyRemaining(ByteBuffer buffer, int expected) {
        if (buffer.remaining() < expected) {
            throw new IllegalArgumentException("Must have at least " + expected + " bytes to write");
        }
    }

    private static void verifyShortValues(int... shortValues) {
        for (int dimen : shortValues) {
            if (dimen > Short.MAX_VALUE || dimen < 0) {
                throw new IllegalArgumentException("Must pass in non-negative short dimensions, not: " + dimen);
            }
        }
    }
}
