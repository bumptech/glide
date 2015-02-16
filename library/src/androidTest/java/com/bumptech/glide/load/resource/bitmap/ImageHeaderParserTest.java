package com.bumptech.glide.load.resource.bitmap;

import static com.bumptech.glide.load.resource.bitmap.ImageHeaderParser.ImageType;
import static org.junit.Assert.assertEquals;

import com.bumptech.glide.testutil.TestResourceUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.robolectric.util.Util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

@RunWith(JUnit4.class)
public class ImageHeaderParserTest {

  private static final byte[] PNG_HEADER_WITH_IHDR_CHUNK =
      new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0xd, 0xa, 0x1a, 0xa, 0x0, 0x0, 0x0, 0xd, 0x49,
          0x48, 0x44, 0x52, 0x0, 0x0, 0x1, (byte) 0x90, 0x0, 0x0, 0x1, 0x2c, 0x8, 0x6 };

  @Test
  public void testCanParsePngType() throws IOException {
    // PNG magic number from: http://en.wikipedia.org/wiki/Portable_Network_Graphics.
    byte[] data = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
    runTest(data, new ParserTestCase() {
      @Override
      public void run(ImageHeaderParser parser) throws IOException {
        assertEquals(ImageType.PNG, parser.getType());
      }
    });
  }

  @Test
  public void testCanParsePngWithAlpha() throws IOException {
    for (int i = 3; i <= 6; i++) {
      byte[] pngHeaderWithIhdrChunk = generatePngHeaderWithIhdr(i);
      runTest(pngHeaderWithIhdrChunk, new ParserTestCase() {
        @Override
        public void run(ImageHeaderParser parser) throws IOException {
          assertEquals(ImageType.PNG_A, parser.getType());
        }
      });
    }
  }

  @Test
  public void testCanParsePngWithoutAlpha() throws IOException {
    for (int i = 0; i < 3; i++) {
      byte[] pngHeaderWithIhdrChunk = generatePngHeaderWithIhdr(i);
      runTest(pngHeaderWithIhdrChunk, new ParserTestCase() {
        @Override
        public void run(ImageHeaderParser parser) throws IOException {
          assertEquals(ImageType.PNG, parser.getType());
        }
      });
    }
  }

  @Test
  public void testCanParseJpegType() throws IOException {
    byte[] data = new byte[] { (byte) 0xFF, (byte) 0xD8 };
    runTest(data, new ParserTestCase() {
      @Override
      public void run(ImageHeaderParser parser) throws IOException {
        assertEquals(ImageType.JPEG, parser.getType());
      }
    });
  }

  @Test
  public void testCanParseGifType() throws IOException {
    byte[] data = new byte[] { 'G', 'I', 'F' };
    runTest(data, new ParserTestCase() {
      @Override
      public void run(ImageHeaderParser parser) throws IOException {
        assertEquals(ImageType.GIF, parser.getType());
      }
    });
  }

  @Test
  public void testReturnsUnknownTypeForUnknownImageHeaders() throws IOException {
    byte[] data = new byte[] { 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0 };
    runTest(data, new ParserTestCase() {
      @Override
      public void run(ImageHeaderParser parser) throws IOException {
        assertEquals(ImageType.UNKNOWN, parser.getType());
      }
    });
  }

  // Test for #286.
  @Test
  public void testHandlesParsingOrientationWithMinimalExifSegment() throws IOException {
    byte[] data =
        Util.readBytes(TestResourceUtil.openResource(getClass(), "short_exif_sample.jpg"));
    runTest(data, new ParserTestCase() {
      @Override
      public void run(ImageHeaderParser parser) throws IOException {
        assertEquals(-1, parser.getOrientation());
      }
    });
  }

  @Test
  public void testReturnsUnknownForEmptyData() throws IOException {
    runTest(new byte[0], new ParserTestCase() {
      @Override
      public void run(ImageHeaderParser parser) throws IOException {
        assertEquals(ImageType.UNKNOWN, parser.getType());
      }
    });
  }

  private interface ParserTestCase {
    void run(ImageHeaderParser parser) throws IOException;
  }

  private static void runTest(byte[] data, ParserTestCase test) throws IOException {
    InputStream is = new ByteArrayInputStream(data);
    ImageHeaderParser parser = new ImageHeaderParser(is);
    test.run(parser);

    ByteBuffer buffer = ByteBuffer.wrap(data);
    parser = new ImageHeaderParser(buffer);
    test.run(parser);
  }

  private static byte[] generatePngHeaderWithIhdr(int bitDepth) {
    byte[] result = new byte[PNG_HEADER_WITH_IHDR_CHUNK.length];
    System.arraycopy(PNG_HEADER_WITH_IHDR_CHUNK, 0, result, 0, PNG_HEADER_WITH_IHDR_CHUNK.length);
    result[result.length - 1] = (byte) bitDepth;
    return result;
  }
}