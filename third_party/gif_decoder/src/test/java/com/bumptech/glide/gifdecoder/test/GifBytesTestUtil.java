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
  // Length in bytes.
  public static final int GRAPHICS_CONTROL_EXTENSION_LENGTH = 8;

  public static int getColorTableLength(int numColors) {
    return 3 * numColors;
  }

  public static int getImageDataSize() {
    // TODO: fill this out.
    return 4;
  }

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

  public static void writeColorTable(ByteBuffer out, int numColors) {
    verifyRemaining(out, getColorTableLength(numColors));
    for (int i = 0; i < numColors; i++) {
      out.put((byte) (0xFF0000 & i));
      out.put((byte) (0x00FF00 & i));
      out.put((byte) (0x0000FF & i));
    }
  }

  public static void writeImageDescriptor(ByteBuffer out, int imageLeft, int imageTop,
      int imageWidth, int imageHeight, boolean hasLct, int numColors) {
    verifyRemaining(out, IMAGE_DESCRIPTOR_LENGTH);
    verifyShortValues(imageLeft, imageTop, imageWidth, imageHeight);

    final byte packed;
    if (hasLct) {
      int size = log2(numColors) - 1;
      packed = (byte) (0x80 | size);
    } else {
      packed = 0x00;
    }

    // Image separator
    out.put((byte) 0x2C);
    out.putShort((short) imageLeft).putShort((short) imageTop).putShort((short) imageWidth)
        .putShort((short) imageHeight).put(packed);
  }

  private static int log2(int num) {
    return (int) Math.round(Math.log(num) / Math.log(2));
  }

  public static void writeHeaderAndLsd(ByteBuffer out, int width, int height, boolean hasGct,
      int gctSize) {
    verifyRemaining(out, HEADER_LENGTH);
    verifyShortValues(width, height);

    // GIF
    out.put((byte) 0x47).put((byte) 0x49).put((byte) 0x46);
    // Version - 89a.
    out.put((byte) 0x38).put((byte) 0x39).put((byte) 0x61);

    /* LSD (Logical Screen Descriptor) **/
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
    byte sortFlag = 0;
    // exponent of size of color table, size = 2^(1 + exponent) - least significant 3 bits.
    byte size = (byte) gctSize;

    byte packed = (byte) (gctFlag | colorResolution | sortFlag | size);
    out.put(packed);

    // Background color index.
    out.put((byte) 0);

    // Pixel aspect ratio.
    out.put((byte) 0);
  }

  public static void writeGraphicsControlExtension(ByteBuffer out, int delayTime) {
    verifyRemaining(out, GRAPHICS_CONTROL_EXTENSION_LENGTH);
    verifyShortValues(delayTime);

    // Extension inducer (constant).
    out.put((byte) 0x21);
    // Graphic control label (constant).
    out.put((byte) 0xF9);
    // Block size (constant).
    out.put((byte) 0x04);
    // Packed (disposal method, user input, transparent color flag)
    out.put((byte) 0x00);

    // Frame delay in 100ths of a second.
    out.putShort((short) delayTime);

    // Transparent color index.
    out.put((byte) 0x00);

    // Block terminator (constant).
    out.put((byte) 0x00);
  }

  private static void verifyRemaining(ByteBuffer buffer, int expected) {
    if (buffer.remaining() < expected) {
      throw new IllegalArgumentException("Must have at least " + expected + " bytes to write");
    }
  }

  private static void verifyShortValues(int... shortValues) {
    for (int dimen : shortValues) {
      if (dimen > Short.MAX_VALUE || dimen < 0) {
        throw new IllegalArgumentException(
            "Must pass in non-negative short dimensions, not: " + dimen);
      }
    }
  }
}
