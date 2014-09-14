package com.bumptech.glide.gifdecoder;

import com.bumptech.glide.gifdecoder.test.GifBytesTestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link com.bumptech.glide.gifdecoder.GifHeaderParser}.
 */
public class GifHeaderParserTest {
    private GifHeaderParser parser;

    @Before
    public void setUp() {
        parser = new GifHeaderParser();
    }

    @Test
    public void testReturnsHeaderWithFormatErrorIfDoesNotStartWithGifHeader() {
        parser.setData("wrong_header".getBytes());
        GifHeader result = parser.parseHeader();
        assertEquals(GifDecoder.STATUS_FORMAT_ERROR, result.status);
    }

    @Test
    public void testCanReadValidHeaderAndLSD() {
        final int width = 10;
        final int height = 20;
        ByteBuffer buffer = ByteBuffer.allocate(GifBytesTestUtil.HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        GifBytesTestUtil.writeHeaderAndLsd(buffer, width, height, false, 0);

        parser.setData(buffer.array());
        GifHeader header = parser.parseHeader();
        assertEquals(width, header.width);
        assertEquals(height, header.height);
        assertFalse(header.gctFlag);
        // 2^(1+0) == 2^1 == 2.
        assertEquals(2, header.gctSize);
        assertEquals(0, header.bgIndex);
        assertEquals(0, header.pixelAspect);
    }

    @Test
    public void testCanParseHeaderOfTestImageWithoutGraphicalExtension() throws IOException {
        byte[] data = readResourceData("gif_without_graphical_control_extension.gif");
        parser.setData(data);
        GifHeader header = parser.parseHeader();
        assertEquals(1, header.frameCount);
        assertNotNull(header.frames.get(0));
        assertEquals(GifDecoder.STATUS_OK, header.status);
    }

    @Test
    public void testCanReadImageDescriptorWithoutGraphicalExtension() {
        ByteBuffer buffer = ByteBuffer.allocate(GifBytesTestUtil.HEADER_LENGTH
                + GifBytesTestUtil.IMAGE_DESCRIPTOR_LENGTH + 4).order(ByteOrder.LITTLE_ENDIAN);
        GifBytesTestUtil.writeHeaderAndLsd(buffer, 1, 1, false, 0);
        GifBytesTestUtil.writeImageDescriptor(buffer, 0, 0, 1, 1);
        GifBytesTestUtil.writeFakeImageData(buffer, 2);

        parser.setData(buffer.array());
        GifHeader header = parser.parseHeader();
        assertEquals(1, header.width);
        assertEquals(1, header.height);
        assertEquals(1, header.frameCount);
        assertNotNull(header.frames.get(0));
    }

    private InputStream openResource(String imageName) throws IOException {
        return getClass().getResourceAsStream("/" + imageName);
    }

    private byte[] readResourceData(String imageName) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        InputStream is = null;
        try {
            is = openResource(imageName);
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore.
                }
            }
        }
        return os.toByteArray();
    }
}