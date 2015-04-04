package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;
import static com.bumptech.glide.load.resource.bitmap.ImageHeaderParser.ImageType;
import static org.junit.Assert.assertEquals;

import com.bumptech.glide.testutil.TestResourceUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class ImageHeaderParserTest {

    private static final byte[] PNG_HEADER_WITH_IHDR_CHUNK = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0xd, 0xa, 0x1a,
            0xa, 0x0, 0x0, 0x0, 0xd, 0x49, 0x48, 0x44, 0x52, 0x0, 0x0, 0x1, (byte) 0x90, 0x0, 0x0, 0x1, 0x2c, 0x8,
            0x6 };

    @Test
    public void testCanParsePngType() throws IOException {
        // PNG magic number from: http://en.wikipedia.org/wiki/Portable_Network_Graphics.
        InputStream is = new ByteArrayInputStream(new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a });
        ImageHeaderParser parser = new ImageHeaderParser(is);
        assertEquals(ImageType.PNG, parser.getType());
    }

    @Test
    public void testCanParsePngWithAlpha() throws IOException {
        for (int i = 3; i <= 6; i++) {
            byte[] pngHeaderWithIhdrChunk = generatePngHeaderWithIhdr(i);
            InputStream is = new ByteArrayInputStream(pngHeaderWithIhdrChunk);
            ImageHeaderParser parser = new ImageHeaderParser(is);
            assertEquals(ImageType.PNG_A, parser.getType());
        }
    }

    @Test
    public void testCanParsePngWithoutAlpha() throws IOException {
        for (int i = 0; i < 3; i++) {
            byte[] pngHeaderWithIhdrChunk = generatePngHeaderWithIhdr(i);
            InputStream is = new ByteArrayInputStream(pngHeaderWithIhdrChunk);
            ImageHeaderParser parser = new ImageHeaderParser(is);
            assertEquals(ImageType.PNG, parser.getType());
        }
    }

    @Test
    public void testCanParseJpegType() throws IOException {
        InputStream is = new ByteArrayInputStream(new byte[] { (byte) 0xFF, (byte) 0xD8 });
        ImageHeaderParser parser = new ImageHeaderParser(is);
        assertEquals(ImageType.JPEG, parser.getType());
    }

    @Test
    public void testCanParseGifType() throws IOException {
        InputStream is = new ByteArrayInputStream(new byte[] { 'G', 'I', 'F' });
        ImageHeaderParser parser = new ImageHeaderParser(is);
        assertEquals(ImageType.GIF, parser.getType());
    }

    @Test
    public void testReturnsUnknownTypeForUnknownImageHeaders() throws IOException {
        InputStream is = new ByteArrayInputStream(new byte[] { 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0 });
        ImageHeaderParser parser = new ImageHeaderParser(is);
        assertEquals(ImageType.UNKNOWN, parser.getType());
    }

    @Test
    public void testReturnsUnknownTypeForEmptyData() throws IOException {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        ImageHeaderParser parser = new ImageHeaderParser(is);
        assertEquals(ImageType.UNKNOWN, parser.getType());
    }

    // Test for #286.
    @Test
    public void testHandlesParsingOrientationWithMinimalExifSegment() throws IOException {
        InputStream is = TestResourceUtil.openResource(getClass(), "short_exif_sample.jpg");
        ImageHeaderParser parser = new ImageHeaderParser(is);
        assertEquals(-1, parser.getOrientation());
    }

    // Test for #387.
    @Test
    public void testHandlesPartialReads() throws IOException {
        InputStream is = TestResourceUtil.openResource(getClass(), "issue387_rotated_jpeg.jpg");
        ImageHeaderParser parser = new ImageHeaderParser(new PartialReadInputStream(is));
        assertThat(parser.getOrientation()).isEqualTo(6);
    }

    // Test for #387.
    @Test
    public void testHandlesPartialSkips() throws IOException {
        InputStream is = TestResourceUtil.openResource(getClass(), "issue387_rotated_jpeg.jpg");
        ImageHeaderParser parser = new ImageHeaderParser(new PartialSkipInputStream(is));
        assertThat(parser.getOrientation()).isEqualTo(6);
    }

    @Test
    public void testHandlesSometimesZeroSkips() throws IOException {
        InputStream is = new ByteArrayInputStream(new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a });
        ImageHeaderParser parser = new ImageHeaderParser(new SometimesZeroSkipInputStream(is));
        assertEquals(ImageType.PNG, parser.getType());
    }

    private static byte[] generatePngHeaderWithIhdr(int bitDepth) {
        byte[] result = new byte[PNG_HEADER_WITH_IHDR_CHUNK.length];
        System.arraycopy(PNG_HEADER_WITH_IHDR_CHUNK, 0, result, 0, PNG_HEADER_WITH_IHDR_CHUNK.length);
        result[result.length - 1] = (byte) bitDepth;
        return result;
    }

    private static class SometimesZeroSkipInputStream extends FilterInputStream {
        boolean returnZeroFlag = true;

        protected SometimesZeroSkipInputStream(InputStream in) {
            super(in);
        }

        @Override
        public long skip(long byteCount) throws IOException {
            if (returnZeroFlag) {
                return 0;
            }
            returnZeroFlag = !returnZeroFlag;
            return super.skip(byteCount);
        }
    }

    private static class PartialSkipInputStream extends FilterInputStream {

        protected PartialSkipInputStream(InputStream in) {
            super(in);
        }

        @Override
        public long skip(long byteCount) throws IOException {
            long toActuallySkip = byteCount / 2;
            if (byteCount == 1) {
                toActuallySkip = 1;
            }
            return super.skip(toActuallySkip);
        }
    }

    private static class PartialReadInputStream extends FilterInputStream {

        protected PartialReadInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
            int toActuallyRead = byteCount / 2;
            if (byteCount == 1) {
                toActuallyRead = 1;
            }
            return super.read(buffer, byteOffset, toActuallyRead);
        }
    }
}