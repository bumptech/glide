package com.bumptech.glide.gifdecoder.test;

import org.junit.Test;

import java.nio.ByteBuffer;

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
    public void testWriteImageDescriptor() {
        ByteBuffer buffer = ByteBuffer.allocate(GifBytesTestUtil.IMAGE_DESCRIPTOR_LENGTH);
        GifBytesTestUtil.writeImageDescriptor(buffer, 10, 9, 8, 7);

        byte[] expected = new byte[] { 0x2C, 0x00, 0x0A, 0x00, 0X09, 0x00, 0x08, 0x000, 0x07, 0x00 };

        assertArrayEquals(expected, buffer.array());
    }

    @Test
    public void testWriteFakeImageData() {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        GifBytesTestUtil.writeFakeImageData(buffer, 2);

        byte[] expected = new byte[] { 0x02, 0x01, 0x01, 0x00 };

        assertArrayEquals(expected, buffer.array());
    }
}
