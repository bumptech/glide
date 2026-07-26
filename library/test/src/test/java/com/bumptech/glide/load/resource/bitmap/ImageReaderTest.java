package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.testutil.TestResourceUtil;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ImageReaderTest {

  private static final String ROTATED_JPEG_RESOURCE_NAME = "issue387_rotated_jpeg.jpg";

  private List<ImageHeaderParser> parsers;
  private LruArrayPool byteArrayPool;

  @Before
  public void setUp() {
    parsers = new ArrayList<>();
    parsers.add(new DefaultImageHeaderParser());
    byteArrayPool = new LruArrayPool();
  }

  @Test
  public void testByteBufferReader_heapBuffer_decodesBitmap() throws IOException {
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
  public void testByteBufferReader_directBuffer_decodesBitmap() throws IOException {
    byte[] imageBytes = openResourceBytes(ROTATED_JPEG_RESOURCE_NAME);
    ByteBuffer buffer = ByteBuffer.allocateDirect(imageBytes.length);
    buffer.put(imageBytes);
    buffer.position(0);
    ImageReader.ByteBufferReader reader =
        new ImageReader.ByteBufferReader(buffer, parsers, byteArrayPool);

    BitmapFactory.Options options = new BitmapFactory.Options();
    Bitmap bitmap = reader.decodeBitmap(options);

    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getWidth()).isGreaterThan(0);
    assertThat(bitmap.getHeight()).isGreaterThan(0);
  }

  @Test
  public void testByteBufferReader_sliceBuffer_decodesBitmap() throws IOException {
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
  public void testByteBufferReader_experimentDisabled_decodesBitmapWithStream() throws IOException {
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
  }

  private byte[] openResourceBytes(String resourceName) throws IOException {
    try (InputStream is = TestResourceUtil.openResource(getClass(), resourceName)) {
      return ByteStreams.toByteArray(is);
    }
  }
}
