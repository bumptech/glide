package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParser.ImageType;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.testutil.TestResourceUtil;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.Util;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class DefaultImageHeaderParserTest {

  private static final byte[] PNG_HEADER_WITH_IHDR_CHUNK =
      new byte[] {
        (byte) 0x89,
        0x50,
        0x4e,
        0x47,
        0xd,
        0xa,
        0x1a,
        0xa,
        0x0,
        0x0,
        0x0,
        0xd,
        0x49,
        0x48,
        0x44,
        0x52,
        0x0,
        0x0,
        0x1,
        (byte) 0x90,
        0x0,
        0x0,
        0x1,
        0x2c,
        0x8,
        0x6
      };

  private ArrayPool byteArrayPool;

  @Before
  public void setUp() {
    byteArrayPool = new LruArrayPool();
  }

  @Test
  public void testCanParsePngType() throws IOException {
    // PNG magic number from: http://en.wikipedia.org/wiki/Portable_Network_Graphics.
    byte[] data = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    runTest(
        data,
        new ParserTestCase() {
          @Override
          public void run(DefaultImageHeaderParser parser, InputStream is, ArrayPool byteArrayPool)
              throws IOException {
            assertEquals(ImageType.PNG, parser.getType(is));
          }

          @Override
          public void run(
              DefaultImageHeaderParser parser, ByteBuffer byteBuffer, ArrayPool byteArrayPool)
              throws IOException {
            assertEquals(ImageType.PNG, parser.getType(byteBuffer));
          }
        });
  }

  @Test
  public void testCanParsePngWithAlpha() throws IOException {
    for (int i = 3; i <= 6; i++) {
      byte[] pngHeaderWithIhdrChunk = generatePngHeaderWithIhdr(i);
      runTest(
          pngHeaderWithIhdrChunk,
          new ParserTestCase() {
            @Override
            public void run(
                DefaultImageHeaderParser parser, InputStream is, ArrayPool byteArrayPool)
                throws IOException {
              assertEquals(ImageType.PNG_A, parser.getType(is));
            }

            @Override
            public void run(
                DefaultImageHeaderParser parser, ByteBuffer byteBuffer, ArrayPool byteArrayPool)
                throws IOException {
              assertEquals(ImageType.PNG_A, parser.getType(byteBuffer));
            }
          });
    }
  }

  @Test
  public void testCanParsePngWithoutAlpha() throws IOException {
    for (int i = 0; i < 3; i++) {
      byte[] pngHeaderWithIhdrChunk = generatePngHeaderWithIhdr(i);
      runTest(
          pngHeaderWithIhdrChunk,
          new ParserTestCase() {
            @Override
            public void run(
                DefaultImageHeaderParser parser, InputStream is, ArrayPool byteArrayPool)
                throws IOException {
              assertEquals(ImageType.PNG, parser.getType(is));
            }

            @Override
            public void run(
                DefaultImageHeaderParser parser, ByteBuffer byteBuffer, ArrayPool byteArrayPool)
                throws IOException {
              assertEquals(ImageType.PNG, parser.getType(byteBuffer));
            }
          });
    }
  }

  @Test
  public void testCanParseJpegType() throws IOException {
    byte[] data = new byte[] {(byte) 0xFF, (byte) 0xD8};
    runTest(
        data,
        new ParserTestCase() {
          @Override
          public void run(DefaultImageHeaderParser parser, InputStream is, ArrayPool byteArrayPool)
              throws IOException {
            assertEquals(ImageType.JPEG, parser.getType(is));
          }

          @Override
          public void run(
              DefaultImageHeaderParser parser, ByteBuffer byteBuffer, ArrayPool byteArrayPool)
              throws IOException {
            assertEquals(ImageType.JPEG, parser.getType(byteBuffer));
          }
        });
  }

  @Test
  public void testCanParseGifType() throws IOException {
    byte[] data = new byte[] {'G', 'I', 'F'};
    runTest(
        data,
        new ParserTestCase() {
          @Override
          public void run(DefaultImageHeaderParser parser, InputStream is, ArrayPool byteArrayPool)
              throws IOException {
            assertEquals(ImageType.GIF, parser.getType(is));
          }

          @Override
          public void run(
              DefaultImageHeaderParser parser, ByteBuffer byteBuffer, ArrayPool byteArrayPool)
              throws IOException {
            assertEquals(ImageType.GIF, parser.getType(byteBuffer));
          }
        });
  }

  @Test
  public void testCanParseWebpWithAlpha() throws IOException {
    byte[] data =
        new byte[] {
          0x52,
          0x49,
          0x46,
          0x46,
          0x3c,
          0x50,
          0x00,
          0x00,
          0x57,
          0x45,
          0x42,
          0x50,
          0x56,
          0x50,
          0x38,
          0x4c,
          0x30,
          0x50,
          0x00,
          0x00,
          0x2f,
          (byte) 0xef,
          (byte) 0x80,
          0x15,
          0x10,
          (byte) 0x8d,
          0x30,
          0x68,
          0x1b,
          (byte) 0xc9,
          (byte) 0x91,
          (byte) 0xb2
        };
    runTest(
        data,
        new ParserTestCase() {
          @Override
          public void run(DefaultImageHeaderParser parser, InputStream is, ArrayPool byteArrayPool)
              throws IOException {
            assertEquals(ImageType.WEBP_A, parser.getType(is));
          }

          @Override
          public void run(
              DefaultImageHeaderParser parser, ByteBuffer byteBuffer, ArrayPool byteArrayPool)
              throws IOException {
            assertEquals(ImageType.WEBP_A, parser.getType(byteBuffer));
          }
        });
  }

  @Test
  public void testCanParseWebpWithoutAlpha() throws IOException {
    byte[] data =
        new byte[] {
          0x52,
          0x49,
          0x46,
          0x46,
          0x72,
          0x1c,
          0x00,
          0x00,
          0x57,
          0x45,
          0x42,
          0x50,
          0x56,
          0x50,
          0x38,
          0x20,
          0x66,
          0x1c,
          0x00,
          0x00,
          0x30,
          0x3c,
          0x01,
          (byte) 0x9d,
          0x01,
          0x2a,
          0x52,
          0x02,
          (byte) 0x94,
          0x03,
          0x00,
          (byte) 0xc7
        };
    runTest(
        data,
        new ParserTestCase() {
          @Override
          public void run(DefaultImageHeaderParser parser, InputStream is, ArrayPool byteArrayPool)
              throws IOException {
            assertEquals(ImageType.WEBP, parser.getType(is));
          }

          @Override
          public void run(
              DefaultImageHeaderParser parser, ByteBuffer byteBuffer, ArrayPool byteArrayPool)
              throws IOException {
            assertEquals(ImageType.WEBP, parser.getType(byteBuffer));
          }
        });
  }

  @Test
  public void testReturnsUnknownTypeForUnknownImageHeaders() throws IOException {
    byte[] data = new byte[] {0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0};
    runTest(
        data,
        new ParserTestCase() {
          @Override
          public void run(DefaultImageHeaderParser parser, InputStream is, ArrayPool byteArrayPool)
              throws IOException {
            assertEquals(ImageType.UNKNOWN, parser.getType(is));
          }

          @Override
          public void run(
              DefaultImageHeaderParser parser, ByteBuffer byteBuffer, ArrayPool byteArrayPool)
              throws IOException {
            assertEquals(ImageType.UNKNOWN, parser.getType(byteBuffer));
          }
        });
  }

  // Test for #286.
  @Test
  public void testHandlesParsingOrientationWithMinimalExifSegment() throws IOException {
    byte[] data =
        Util.readBytes(TestResourceUtil.openResource(getClass(), "short_exif_sample.jpg"));
    runTest(
        data,
        new ParserTestCase() {
          @Override
          public void run(DefaultImageHeaderParser parser, InputStream is, ArrayPool byteArrayPool)
              throws IOException {
            assertEquals(-1, parser.getOrientation(is, byteArrayPool));
          }

          @Override
          public void run(
              DefaultImageHeaderParser parser, ByteBuffer byteBuffer, ArrayPool byteArrayPool)
              throws IOException {
            assertEquals(-1, parser.getOrientation(byteBuffer, byteArrayPool));
          }
        });
  }

  @Test
  public void testReturnsUnknownForEmptyData() throws IOException {
    runTest(
        new byte[0],
        new ParserTestCase() {
          @Override
          public void run(DefaultImageHeaderParser parser, InputStream is, ArrayPool byteArrayPool)
              throws IOException {
            assertEquals(ImageType.UNKNOWN, parser.getType(is));
          }

          @Override
          public void run(
              DefaultImageHeaderParser parser, ByteBuffer byteBuffer, ArrayPool byteArrayPool)
              throws IOException {
            assertEquals(ImageType.UNKNOWN, parser.getType(byteBuffer));
          }
        });
  }

  // Test for #387.
  @Test
  public void testHandlesPartialReads() throws IOException {
    InputStream is = TestResourceUtil.openResource(getClass(), "issue387_rotated_jpeg.jpg");
    DefaultImageHeaderParser parser = new DefaultImageHeaderParser();
    assertThat(parser.getOrientation(new PartialReadInputStream(is), byteArrayPool)).isEqualTo(6);
  }

  // Test for #387.
  @Test
  public void testHandlesPartialSkips() throws IOException {
    InputStream is = TestResourceUtil.openResource(getClass(), "issue387_rotated_jpeg.jpg");
    DefaultImageHeaderParser parser = new DefaultImageHeaderParser();
    assertThat(parser.getOrientation(new PartialSkipInputStream(is), byteArrayPool)).isEqualTo(6);
  }

  @Test
  public void testHandlesSometimesZeroSkips() throws IOException {
    InputStream is =
        new ByteArrayInputStream(
            new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
    DefaultImageHeaderParser parser = new DefaultImageHeaderParser();
    assertEquals(ImageType.PNG, parser.getType(new SometimesZeroSkipInputStream(is)));
  }

  @Test
  public void getOrientation_withExifSegmentLessThanLength_returnsUnknown() throws IOException {
    ByteBuffer jpegHeaderBytes = getExifMagicNumber();
    byte[] data =
        new byte[] {
          jpegHeaderBytes.get(0),
          jpegHeaderBytes.get(1),
          (byte) DefaultImageHeaderParser.SEGMENT_START_ID,
          (byte) DefaultImageHeaderParser.EXIF_SEGMENT_TYPE,
          // SEGMENT_LENGTH
          (byte) 0xFF,
          (byte) 0xFF,
        };
    ByteBuffer byteBuffer = ByteBuffer.wrap(data);
    DefaultImageHeaderParser parser = new DefaultImageHeaderParser();
    assertEquals(
        ImageHeaderParser.UNKNOWN_ORIENTATION, parser.getOrientation(byteBuffer, byteArrayPool));
  }

  @Test
  public void getOrientation_withNonExifSegmentLessThanLength_returnsUnknown() throws IOException {
    ByteBuffer jpegHeaderBytes = getExifMagicNumber();
    byte[] data =
        new byte[] {
          jpegHeaderBytes.get(0),
          jpegHeaderBytes.get(1),
          (byte) DefaultImageHeaderParser.SEGMENT_START_ID,
          // SEGMENT_TYPE (NOT EXIF_SEGMENT_TYPE)
          (byte) 0xE5,
          // SEGMENT_LENGTH
          (byte) 0xFF,
          (byte) 0xFF,
        };
    ByteBuffer byteBuffer = ByteBuffer.wrap(data);
    DefaultImageHeaderParser parser = new DefaultImageHeaderParser();
    assertEquals(
        ImageHeaderParser.UNKNOWN_ORIENTATION, parser.getOrientation(byteBuffer, byteArrayPool));
  }

  @Test
  public void getOrientation_withExifSegmentAndPreambleButLessThanLength_returnsUnknown()
      throws IOException {
    ByteBuffer jpegHeaderBytes = getExifMagicNumber();
    ByteBuffer exifSegmentPreamble =
        ByteBuffer.wrap(DefaultImageHeaderParser.JPEG_EXIF_SEGMENT_PREAMBLE_BYTES);

    ByteBuffer data = ByteBuffer.allocate(2 + 1 + 1 + 2 + exifSegmentPreamble.capacity());
    data.put(jpegHeaderBytes)
        .put((byte) DefaultImageHeaderParser.SEGMENT_START_ID)
        .put((byte) DefaultImageHeaderParser.EXIF_SEGMENT_TYPE)
        // SEGMENT_LENGTH, add two because length includes the segment length short, and one to go
        // beyond the preamble bytes length for the test.
        .putShort(
            (short) (DefaultImageHeaderParser.JPEG_EXIF_SEGMENT_PREAMBLE_BYTES.length + 2 + 1))
        .put(DefaultImageHeaderParser.JPEG_EXIF_SEGMENT_PREAMBLE_BYTES);

    data.position(0);
    DefaultImageHeaderParser parser = new DefaultImageHeaderParser();
    assertEquals(ImageHeaderParser.UNKNOWN_ORIENTATION, parser.getOrientation(data, byteArrayPool));
  }

  @Test
  public void getOrientation_withExifSegmentAndPreambleBetweenLengthAndExpected_returnsUnknown()
      throws IOException {
    ByteBuffer jpegHeaderBytes = getExifMagicNumber();
    ByteBuffer exifSegmentPreamble =
        ByteBuffer.wrap(DefaultImageHeaderParser.JPEG_EXIF_SEGMENT_PREAMBLE_BYTES);

    ByteBuffer data = ByteBuffer.allocate(2 + 1 + 1 + 2 + exifSegmentPreamble.capacity() + 2 + 1);
    data.put(jpegHeaderBytes)
        .put((byte) DefaultImageHeaderParser.SEGMENT_START_ID)
        .put((byte) DefaultImageHeaderParser.EXIF_SEGMENT_TYPE)
        // SEGMENT_LENGTH, add two because length includes the segment length short, and one to go
        // beyond the preamble bytes length for the test.
        .putShort(
            (short) (DefaultImageHeaderParser.JPEG_EXIF_SEGMENT_PREAMBLE_BYTES.length + 2 + 1))
        .put(DefaultImageHeaderParser.JPEG_EXIF_SEGMENT_PREAMBLE_BYTES);

    data.position(0);
    DefaultImageHeaderParser parser = new DefaultImageHeaderParser();
    assertEquals(ImageHeaderParser.UNKNOWN_ORIENTATION, parser.getOrientation(data, byteArrayPool));
  }

  private static ByteBuffer getExifMagicNumber() {
    ByteBuffer jpegHeaderBytes = ByteBuffer.allocate(2);
    jpegHeaderBytes.putShort((short) DefaultImageHeaderParser.EXIF_MAGIC_NUMBER);
    jpegHeaderBytes.position(0);
    return jpegHeaderBytes;
  }

  private interface ParserTestCase {
    void run(DefaultImageHeaderParser parser, InputStream is, ArrayPool byteArrayPool)
        throws IOException;

    void run(DefaultImageHeaderParser parser, ByteBuffer byteBuffer, ArrayPool byteArrayPool)
        throws IOException;
  }

  private static void runTest(byte[] data, ParserTestCase test) throws IOException {
    InputStream is = new ByteArrayInputStream(data);
    DefaultImageHeaderParser parser = new DefaultImageHeaderParser();
    test.run(parser, is, new LruArrayPool());

    ByteBuffer buffer = ByteBuffer.wrap(data);
    parser = new DefaultImageHeaderParser();
    test.run(parser, buffer, new LruArrayPool());
  }

  private static byte[] generatePngHeaderWithIhdr(int bitDepth) {
    byte[] result = new byte[PNG_HEADER_WITH_IHDR_CHUNK.length];
    System.arraycopy(PNG_HEADER_WITH_IHDR_CHUNK, 0, result, 0, PNG_HEADER_WITH_IHDR_CHUNK.length);
    result[result.length - 1] = (byte) bitDepth;
    return result;
  }

  private static class SometimesZeroSkipInputStream extends FilterInputStream {
    boolean returnZeroFlag = true;

    SometimesZeroSkipInputStream(InputStream in) {
      super(in);
    }

    @Override
    public long skip(long byteCount) throws IOException {
      final long result;
      if (returnZeroFlag) {
        result = 0;
      } else {
        result = super.skip(byteCount);
      }
      returnZeroFlag = !returnZeroFlag;
      return result;
    }
  }

  private static class PartialSkipInputStream extends FilterInputStream {

    PartialSkipInputStream(InputStream in) {
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

    PartialReadInputStream(InputStream in) {
      super(in);
    }

    @Override
    public int read(@NonNull byte[] buffer, int byteOffset, int byteCount) throws IOException {
      int toActuallyRead = byteCount / 2;
      if (byteCount == 1) {
        toActuallyRead = 1;
      }
      return super.read(buffer, byteOffset, toActuallyRead);
    }
  }
}
