package com.bumptech.glide.gifdecoder.test;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;

/**
 * Tests for {@link com.bumptech.glide.gifdecoder.test.GifBytesTestUtil}.
 */
public class GifBytesTestUtilTest {

    @Test
    public void testWriteHeaderAndLsdWithoutGct() {
        ByteBuffer buffer = ByteBuffer.allocate(GifBytesTestUtil.HEADER_LENGTH);
        GifBytesTestUtil.writeHeaderAndLsd(buffer, 8, 16, false, 0);

        byte[] expected = new byte[] { 0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x00, 0x08, 0x00, 0x10, 0x20, 0x00, 0x00};

        assertArrayEquals(expected, buffer.array());
    }

    @Test
    public void testWriteHeaderAndLsdWithGct() {
        ByteBuffer buffer = ByteBuffer.allocate(GifBytesTestUtil.HEADER_LENGTH);
        GifBytesTestUtil.writeHeaderAndLsd(buffer, 8, 16, true, 4);

        byte[] expected = new byte[] { 0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x00, 0x08, 0x00, 0x10, (byte) 0xA4, 0x00,
                0x00};

        assertArrayEquals(expected, buffer.array());
    }

    @Test
    public void testWriteImageDescriptorWithoutColorTable() {
        ByteBuffer buffer = ByteBuffer.allocate(GifBytesTestUtil.IMAGE_DESCRIPTOR_LENGTH);
        GifBytesTestUtil.writeImageDescriptor(buffer, 10, 9, 8, 7, false, 0);

        byte[] expected = new byte[] {
                // Image separator.
                0x2C,
                // Image left.
                0x00, 0x0A,
                // Image right.
                0x00, 0X09,
                // Image width.
                0x00, 0x08,
                // Image height.
                0x00, 0x07,
                // Packed field.
                0x00
        };

        assertArrayEquals(expected, buffer.array());
    }

    @Test
    public void testWriteImageDescriptorWithColorTable() {
        ByteBuffer buffer = ByteBuffer.allocate(GifBytesTestUtil.IMAGE_DESCRIPTOR_LENGTH);
        GifBytesTestUtil.writeImageDescriptor(buffer, 10, 9, 8, 7, true, 4);

        byte packedField =
                // Set LCT flag
                (byte) 0x80
                // Size of color table (2^(N + 1) == 4)
                | 0x01;

        byte[] expected = new byte[] {
                // Image separator.
                0x2C,
                // Image left.
                0x00, 0x0A,
                // Image right.
                0x00, 0X09,
                // Image width.
                0x00, 0x08,
                // Image height.
                0x00, 0x07,
                packedField
        };

        assertArrayEquals(expected, buffer.array());
    }

    @Test
    public void testWriteColorTable() {
        final int numColors = 4;
        ByteBuffer buffer = ByteBuffer.allocate(GifBytesTestUtil.getColorTableLength(numColors));
        GifBytesTestUtil.writeColorTable(buffer, numColors);

        byte[] expected = new byte[] {
                // First color.
                0x00, 0x00, 0x00,
                // Second color.
                0x00, 0x00, 0x01,
                // Third color.
                0x00, 0x00, 0x02,
                // Fourth color.
                0x00, 0x00, 0x03,
        };


        assertArrayEquals("expected=" + Arrays.toString(expected) + " received=" + Arrays.toString(buffer.array()),
                expected, buffer.array());
    }

    @Test
    public void testWriteFakeImageData() {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        GifBytesTestUtil.writeFakeImageData(buffer, 2);

        byte[] expected = new byte[] { 0x02, 0x01, 0x01, 0x00 };

        assertArrayEquals(expected, buffer.array());
    }
}
