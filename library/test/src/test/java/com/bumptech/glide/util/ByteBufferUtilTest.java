package com.bumptech.glide.util;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class ByteBufferUtilTest {
  private static final int BUFFER_SIZE = 16384;

  @Test
  public void testFromStream_small_direct() throws IOException {
    testFromStream(4, false /* useHeapBuffer */);
  }

  @Test
  public void testFromStream_small_heap() throws IOException {
    testFromStream(4, true /* useHeapBuffer */);
  }

  @Test
  public void testFromStream_empty_direct() throws IOException {
    testFromStream(0, false /* useHeapBuffer */);
  }

  @Test
  public void testFromStream_empty_heap() throws IOException {
    testFromStream(0, true /* useHeapBuffer */);
  }

  @Test
  public void testFromStream_bufferAndAHalf_direct() throws IOException {
    testFromStream(BUFFER_SIZE + BUFFER_SIZE / 2, false /* useHeapBuffer */);
  }

  @Test
  public void testFromStream_bufferAndAHalf_heap() throws IOException {
    testFromStream(BUFFER_SIZE + BUFFER_SIZE / 2, true /* useHeapBuffer */);
  }

  @Test
  public void testFromStream_massive_direct() throws IOException {
    testFromStream(12 * BUFFER_SIZE + 12345, false /* useHeapBuffer */);
  }

  @Test
  public void testFromStream_massive_heap() throws IOException {
    testFromStream(12 * BUFFER_SIZE + 12345, true /* useHeapBuffer */);
  }

  private void testFromStream(int dataLength, boolean useHeapBuffer) throws IOException {
    byte[] bytes = createByteData(dataLength);
    InputStream byteStream = new ByteArrayInputStream(bytes);
    ByteBuffer byteBuffer = ByteBufferUtil.fromStream(byteStream, useHeapBuffer);
    assertByteBufferContents(byteBuffer, bytes);
    assertEquals(useHeapBuffer, !byteBuffer.isDirect());
    byteStream.close();
  }

  private byte[] createByteData(int size) {
    byte[] bytes = new byte[size];

    // Put some arbitrary bytes in there.
    for (int i = 0; i < size; i++) {
      bytes[i] = (byte) (i % 4);
    }

    return bytes;
  }

  private void assertByteBufferContents(ByteBuffer buffer, byte[] expectedBytes) {
    assertEquals(expectedBytes.length, buffer.limit());
    for (int i = 0; i < expectedBytes.length; i++) {
      assertEquals(expectedBytes[i], buffer.get(i));
    }
  }
}
