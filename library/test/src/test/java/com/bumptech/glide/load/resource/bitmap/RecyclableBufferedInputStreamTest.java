package com.bumptech.glide.load.resource.bitmap;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// Not required in tests.
@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class RecyclableBufferedInputStreamTest {

  private static final int DATA_SIZE = 30;
  private static final int BUFFER_SIZE = 10;

  private RecyclableBufferedInputStream stream;
  private byte[] data;
  private ArrayPool byteArrayPool;

  @Before
  public void setUp() {
    data = new byte[DATA_SIZE];
    for (int i = 0; i < DATA_SIZE; i++) {
      data[i] = (byte) i;
    }

    byteArrayPool = new LruArrayPool();
    InputStream wrapped = new ByteArrayInputStream(data);
    stream = new RecyclableBufferedInputStream(wrapped, byteArrayPool, BUFFER_SIZE);
  }

  @Test
  public void returnsTrueForMarkSupported() {
    assertTrue(stream.markSupported());
  }

  @Test
  public void canReadIndividualBytes() throws IOException {
    for (int i = 0; i < data.length; i++) {
      assertEquals(i, stream.read());
    }
    assertEquals(-1, stream.read());
  }

  @Test
  public void canReadBytesInBulkLargerThanBufferSize() throws IOException {
    byte[] buffer = new byte[DATA_SIZE];
    assertEquals(DATA_SIZE, stream.read(buffer, 0, DATA_SIZE));
    for (int i = 0; i < DATA_SIZE; i++) {
      assertEquals(i, buffer[i]);
    }
  }

  @Test
  public void canReadBytesInBulkSmallerThanBufferSize() throws IOException {
    int toRead = BUFFER_SIZE / 2;
    byte[] buffer = new byte[toRead];
    assertEquals(toRead, stream.read(buffer, 0, toRead));
    for (int i = 0; i < toRead; i++) {
      assertEquals(i, buffer[i]);
    }
  }

  @Test
  public void readingZeroBytesIntoBufferReadsZeroBytes() throws IOException {
    // Make sure the next value is not 0.
    stream.read();
    byte[] buffer = new byte[BUFFER_SIZE];
    assertEquals(0, stream.read(buffer, 0, 0));

    for (int i = 0; i < BUFFER_SIZE; i++) {
      assertEquals(0, buffer[i]);
    }
  }

  @Test
  public void canReadIntoBufferLargerThanDataSize() throws IOException {
    int toRead = DATA_SIZE * 2;
    byte[] buffer = new byte[toRead];
    assertEquals(DATA_SIZE, stream.read(buffer, 0, toRead));
    for (int i = 0; i < DATA_SIZE; i++) {
      assertEquals(i, buffer[i]);
    }
    for (int i = DATA_SIZE; i < toRead; i++) {
      assertEquals(0, buffer[i]);
    }
  }

  @Test
  public void canReadBytesInBulkWithLimit() throws IOException {
    int toRead = BUFFER_SIZE / 2;
    byte[] buffer = new byte[BUFFER_SIZE];
    assertEquals(toRead, stream.read(buffer, 0, toRead));

    // 0, 1, 2, 3, 4, 0, 0, 0, 0, 0
    for (int i = 0; i < toRead; i++) {
      assertEquals(i, buffer[i]);
    }
    for (int i = toRead; i < BUFFER_SIZE; i++) {
      assertEquals(0, buffer[i]);
    }
  }

  @Test
  public void canReadBytesInBulkWithOffset() throws IOException {
    int toRead = BUFFER_SIZE / 2;
    byte[] buffer = new byte[BUFFER_SIZE];
    assertEquals(toRead, stream.read(buffer, BUFFER_SIZE - toRead, toRead));
    // 0, 0, 0, 0, 0, 0, 1, 2, 3, 4
    for (int i = 0; i < toRead; i++) {
      assertEquals(0, buffer[i]);
    }
    for (int i = toRead; i < BUFFER_SIZE; i++) {
      assertEquals(i - toRead, buffer[i]);
    }
  }

  @Test
  public void canReadBytesInBulkWhenSomeButNotAllBytesAreInBuffer() throws IOException {
    stream.read();
    byte[] buffer = new byte[BUFFER_SIZE];
    assertEquals(BUFFER_SIZE, stream.read(buffer, 0, BUFFER_SIZE));
    for (int i = 1; i < BUFFER_SIZE + 1; i++) {
      assertEquals(i, buffer[i - 1]);
    }
  }

  @Test
  public void canSkipBytes() throws IOException {
    int toSkip = data.length / 2;
    assertEquals(toSkip, stream.skip(toSkip));
    for (int i = toSkip; i < data.length; i++) {
      assertEquals(i, stream.read());
    }
    assertEquals(-1, stream.read());
  }

  @Test
  public void skipReturnsZeroIfSkipByteCountIsZero() throws IOException {
    assertEquals(0, stream.skip(0));
    assertEquals(0, stream.read());
  }

  @Test
  public void skipReturnsZeroIfSkipByteCountIsNegative() throws IOException {
    assertEquals(0, stream.skip(-13));
    assertEquals(0, stream.read());
  }

  @Test
  public void closeClosesWrappedStream() throws IOException {
    CloseTrackingInputStream wrapped = new CloseTrackingInputStream(/* throwOnClose= */ false);
    stream = new RecyclableBufferedInputStream(wrapped, byteArrayPool);

    stream.close();

    assertThat(wrapped.closeCount).isEqualTo(1);
  }

  @Test
  public void canSafelyBeClosedMultipleTimes() throws IOException {
    CloseTrackingInputStream wrapped = new CloseTrackingInputStream(/* throwOnClose= */ false);
    stream = new RecyclableBufferedInputStream(wrapped, byteArrayPool);

    stream.close();
    stream.close();
    stream.close();

    assertThat(wrapped.closeCount).isEqualTo(1);
  }

  @Test
  public void canMarkAndReset() throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    stream.mark(BUFFER_SIZE);
    assertEquals(BUFFER_SIZE, stream.read(buffer, 0, BUFFER_SIZE));
    for (int i = 0; i < BUFFER_SIZE; i++) {
      assertEquals(i, buffer[i]);
    }
    Arrays.fill(buffer, (byte) 0);
    stream.reset();

    assertEquals(BUFFER_SIZE, stream.read(buffer, 0, BUFFER_SIZE));
    for (int i = 0; i < BUFFER_SIZE; i++) {
      assertEquals(i, buffer[i]);
    }
  }

  @Test
  public void canResetRepeatedlyAfterMarking() throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    stream.mark(BUFFER_SIZE);
    for (int repeat = 0; repeat < 10; repeat++) {
      assertEquals(BUFFER_SIZE, stream.read(buffer, 0, BUFFER_SIZE));
      for (int i = 0; i < BUFFER_SIZE; i++) {
        assertEquals(i, buffer[i]);
      }
      stream.reset();
    }
  }

  @Test
  public void canMarkInMiddleOfBufferAndStillReadUpToBufferLengthBeforeResetting()
      throws IOException {
    int markPos = BUFFER_SIZE / 2;
    for (int i = 0; i < markPos; i++) {
      stream.read();
    }
    stream.mark(BUFFER_SIZE);

    for (int i = 0; i < BUFFER_SIZE; i++) {
      stream.read();
    }

    stream.reset();
    assertEquals(markPos, stream.read());
  }

  @Test
  public void availableReturnsWrappedAvailableIfNoBytesRead() throws IOException {
    assertEquals(DATA_SIZE, stream.available());
  }

  @Test
  public void availableIncludesDataInBufferAndWrappedAvailableIfBytesRead() throws IOException {
    stream.read();
    assertEquals(DATA_SIZE - 1, stream.available());
  }

  @Test
  public void closeThrowsIfWrappedStreamThrowsOnClose() {
    InputStream wrapped = new CloseTrackingInputStream(/* throwOnClose= */ true);
    stream = new RecyclableBufferedInputStream(wrapped, byteArrayPool);

    assertThrows(IOException.class, () -> stream.close());
  }

  @Test
  public void availableThrowsIfStreamIsClosed() throws IOException {
    stream.close();
    assertThrows(IOException.class, () -> stream.available());
  }

  @Test
  public void readThrowsIfStreamIsClosed() throws IOException {
    stream.close();
    assertThrows(IOException.class, () -> stream.read());
  }

  @Test
  public void readBulkThrowsIfStreamIsClosed() throws IOException {
    stream.close();
    assertThrows(IOException.class, () -> stream.read(new byte[1], 0, 1));
  }

  @Test
  public void resetThrowsIfStreamIsClosed() throws IOException {
    stream.close();
    assertThrows(IOException.class, () -> stream.reset());
  }

  @Test
  public void skipThrowsIfStreamIsClosed() throws IOException {
    stream.close();
    assertThrows(IOException.class, () -> stream.skip(10));
  }

  @Test
  public void resetThrowsIfMarkNotSet() {
    assertThrows(RecyclableBufferedInputStream.InvalidMarkException.class, () -> stream.reset());
  }

  @Test
  public void resetThrowsIfMarkIsInvalid() throws IOException {
    stream.mark(1);
    stream.read(new byte[BUFFER_SIZE], 0, BUFFER_SIZE);
    stream.read();
    assertThrows(RecyclableBufferedInputStream.InvalidMarkException.class, () -> stream.reset());
  }

  private static final class CloseTrackingInputStream extends InputStream {
    private final boolean throwOnClose;
    int closeCount;

    CloseTrackingInputStream(boolean throwOnClose) {
      this.throwOnClose = throwOnClose;
    }

    @Override
    public int read() {
      return -1;
    }

    @Override
    public void close() throws IOException {
      closeCount++;
      if (throwOnClose) {
        throw new IOException();
      }
    }
  }
}
