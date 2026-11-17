package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build.VERSION_CODES;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.testutil.TestResourceUtil;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class ImageReaderTest {

  private static final String ROTATED_JPEG_RESOURCE_NAME = "issue387_rotated_jpeg.jpg";
  // Exceeds Skia's 4 KB GIF read buffer so seeking back before pre-frame extensions fails.
  private static final int LARGE_EXTENSION_PAYLOAD_SIZE = 28 * 1024;

  private List<ImageHeaderParser> parsers;
  private LruArrayPool byteArrayPool;

  @Before
  public void setUp() {
    parsers = new ArrayList<>();
    parsers.add(new DefaultImageHeaderParser());
    byteArrayPool = new LruArrayPool();
  }

  @Test
  public void byteBufferReader_heapBuffer_decodesBitmap() throws IOException {
    byte[] imageBytes = openResourceBytes(ROTATED_JPEG_RESOURCE_NAME);
    ByteBuffer buffer = ByteBuffer.wrap(imageBytes);
    ImageReader.ByteBufferReader reader =
        new ImageReader.ByteBufferReader(buffer, parsers, byteArrayPool);

    BitmapFactory.Options options = new BitmapFactory.Options();
    Bitmap bitmap = reader.decodeBitmap(options);

    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getWidth()).isGreaterThan(0);
    assertThat(bitmap.getHeight()).isGreaterThan(0);
  }

  @Test
  public void byteBufferReader_directBuffer_decodesWithByteBufferStream() throws IOException {
    byte[] imageBytes = openResourceBytes(ROTATED_JPEG_RESOURCE_NAME);
    ByteBuffer buffer = ByteBuffer.allocateDirect(imageBytes.length);
    buffer.put(imageBytes);
    buffer.position(0);
    ImageReader.ByteBufferReader reader =
        new ImageReader.ByteBufferReader(
            buffer, parsers, byteArrayPool, /* enableDirectByteBufferDecoding= */ true);

    BitmapFactory.Options options = new BitmapFactory.Options();
    Bitmap bitmap = reader.decodeBitmap(options);

    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getWidth()).isGreaterThan(0);
    assertThat(bitmap.getHeight()).isGreaterThan(0);
    // ByteBufferStream advances buffer.position() to its limit. Falling back to a copy rewinds the
    // buffer and copies from a duplicate, which would leave buffer.position() at 0.
    assertThat(buffer.position()).isEqualTo(buffer.limit());
  }

  @Test
  public void byteBufferReader_directBuffer_decodesBoundsWithByteBufferStream() throws IOException {
    ByteBuffer buffer = toDirectBuffer(openResourceBytes(ROTATED_JPEG_RESOURCE_NAME));
    ImageReader.ByteBufferReader reader =
        new ImageReader.ByteBufferReader(
            buffer, parsers, byteArrayPool, /* enableDirectByteBufferDecoding= */ true);

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    reader.decodeBitmap(options);

    assertThat(options.outWidth).isGreaterThan(0);
    assertThat(options.outHeight).isGreaterThan(0);
    // Bounds-only decodes always return null. Treating that as a failure would fall back to a
    // copy, which rewinds the buffer to position 0.
    assertThat(buffer.position()).isGreaterThan(0);
  }

  @Test
  public void byteBufferReader_sliceBuffer_decodesBitmap() throws IOException {
    byte[] imageBytes = openResourceBytes(ROTATED_JPEG_RESOURCE_NAME);
    ByteBuffer buffer = ByteBuffer.allocate(imageBytes.length + 10);
    buffer.position(5);
    buffer.put(imageBytes);
    buffer.position(5);
    buffer.limit(5 + imageBytes.length);
    ByteBuffer slice = buffer.slice();
    ImageReader.ByteBufferReader reader =
        new ImageReader.ByteBufferReader(slice, parsers, byteArrayPool);

    BitmapFactory.Options options = new BitmapFactory.Options();
    Bitmap bitmap = reader.decodeBitmap(options);

    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getWidth()).isGreaterThan(0);
    assertThat(bitmap.getHeight()).isGreaterThan(0);
  }

  @Test
  public void byteBufferReader_readOnlyHeapBuffer_decodesWithByteBufferStream() throws IOException {
    byte[] imageBytes = openResourceBytes(ROTATED_JPEG_RESOURCE_NAME);
    ByteBuffer buffer = ByteBuffer.wrap(imageBytes).asReadOnlyBuffer();
    ImageReader.ByteBufferReader reader =
        new ImageReader.ByteBufferReader(
            buffer, parsers, byteArrayPool, /* enableDirectByteBufferDecoding= */ true);

    BitmapFactory.Options options = new BitmapFactory.Options();
    Bitmap bitmap = reader.decodeBitmap(options);

    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getWidth()).isGreaterThan(0);
    assertThat(bitmap.getHeight()).isGreaterThan(0);
    // ByteBufferStream advances buffer.position() to its limit. Falling back to a copy rewinds the
    // buffer and copies from a duplicate, which would leave buffer.position() at 0.
    assertThat(buffer.position()).isEqualTo(buffer.limit());
  }

  @Test
  public void byteBufferReader_experimentDisabled_decodesBitmapWithStream() throws IOException {
    byte[] imageBytes = openResourceBytes(ROTATED_JPEG_RESOURCE_NAME);
    ByteBuffer buffer = ByteBuffer.wrap(imageBytes);
    ImageReader.ByteBufferReader reader =
        new ImageReader.ByteBufferReader(
            buffer, parsers, byteArrayPool, /* enableDirectByteBufferDecoding= */ false);

    BitmapFactory.Options options = new BitmapFactory.Options();
    Bitmap bitmap = reader.decodeBitmap(options);

    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getWidth()).isGreaterThan(0);
    assertThat(bitmap.getHeight()).isGreaterThan(0);
    assertThat(buffer.position()).isEqualTo(buffer.limit());
  }

  // Since Android V, BitmapFactory.decodeStream returns null for GIFs whose pre-frame extension
  // blocks exceed Skia's 4 KB GIF read buffer.
  @Test
  @Config(sdk = VERSION_CODES.VANILLA_ICE_CREAM)
  public void byteBufferReader_directBuffer_gifWithLargeMetadata_fallsBackToCopy() {
    ByteBuffer buffer = toDirectBuffer(gifWithApplicationExtension(LARGE_EXTENSION_PAYLOAD_SIZE));
    ImageReader.ByteBufferReader reader =
        new ImageReader.ByteBufferReader(
            buffer, parsers, byteArrayPool, /* enableDirectByteBufferDecoding= */ true);

    BitmapFactory.Options options = new BitmapFactory.Options();
    Bitmap bitmap = reader.decodeBitmap(options);

    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getWidth()).isEqualTo(1);
    assertThat(bitmap.getHeight()).isEqualTo(1);
  }

  @Test
  @Config(sdk = VERSION_CODES.VANILLA_ICE_CREAM)
  public void byteBufferReader_directBuffer_gifWithLargeMetadata_decodesBounds() {
    ByteBuffer buffer = toDirectBuffer(gifWithApplicationExtension(LARGE_EXTENSION_PAYLOAD_SIZE));
    ImageReader.ByteBufferReader reader =
        new ImageReader.ByteBufferReader(
            buffer, parsers, byteArrayPool, /* enableDirectByteBufferDecoding= */ true);

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    reader.decodeBitmap(options);

    assertThat(options.outWidth).isEqualTo(1);
    assertThat(options.outHeight).isEqualTo(1);
  }

  private static ByteBuffer toDirectBuffer(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
    buffer.put(bytes);
    buffer.position(0);
    return buffer;
  }

  /**
   * Returns a 1x1 GIF with an application extension of {@code payloadSize} bytes before its only
   * frame.
   */
  private static byte[] gifWithApplicationExtension(int payloadSize) {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    // Header, logical screen descriptor and a 2-entry global color table.
    writeBytes(os, 'G', 'I', 'F', '8', '9', 'a', 1, 0, 1, 0, 0x80, 0, 0);
    writeBytes(os, 0, 0, 0, 0xFF, 0xFF, 0xFF);
    // Generic GIF89a application extension split into 255-byte data sub-blocks.
    writeBytes(os, 0x21, 0xFF, 11, 'T', 'E', 'S', 'T', '_', 'A', 'P', 'P', 1, 0, 0);
    for (int remaining = payloadSize; remaining > 0; remaining -= 255) {
      int subBlockSize = Math.min(255, remaining);
      os.write(subBlockSize);
      os.write(new byte[subBlockSize], /* off= */ 0, subBlockSize);
    }
    os.write(0);
    // Image descriptor and LZW data for a single pixel, followed by the trailer.
    writeBytes(os, 0x2C, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 0x44, 1, 0, 0x3B);
    return os.toByteArray();
  }

  private static void writeBytes(ByteArrayOutputStream os, int... bytes) {
    for (int b : bytes) {
      os.write(b);
    }
  }

  private byte[] openResourceBytes(String resourceName) throws IOException {
    try (InputStream is = TestResourceUtil.openResource(getClass(), resourceName)) {
      return ByteStreams.toByteArray(is);
    }
  }
}
