package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
@Config(sdk = 18)
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
  public void testReturnsTrueForMarkSupported() {
    assertTrue(stream.markSupported());
  }

  @Test
  public void testCanReadIndividualBytes() throws IOException {
    for (int i = 0; i < data.length; i++) {
      assertEquals(i, stream.read());
    }
    assertEquals(-1, stream.read());
  }

  @Test
  public void testCanReadBytesInBulkLargerThanBufferSize() throws IOException {
    byte[] buffer = new byte[DATA_SIZE];
    assertEquals(DATA_SIZE, stream.read(buffer, 0, DATA_SIZE));
    for (int i = 0; i < DATA_SIZE; i++) {
      assertEquals(i, buffer[i]);
    }
  }

  @Test
  public void testCanReadBytesInBulkSmallerThanBufferSize() throws IOException {
    int toRead = BUFFER_SIZE / 2;
    byte[] buffer = new byte[toRead];
    assertEquals(toRead, stream.read(buffer, 0, toRead));
    for (int i = 0; i < toRead; i++) {
      assertEquals(i, buffer[i]);
    }
  }

  @Test
  public void testReadingZeroBytesIntoBufferReadsZeroBytes() throws IOException {
    // Make sure the next value is not 0.
    stream.read();
    byte[] buffer = new byte[BUFFER_SIZE];
    assertEquals(0, stream.read(buffer, 0, 0));

    for (int i = 0; i < BUFFER_SIZE; i++) {
      assertEquals(0, buffer[i]);
    }
  }

  @Test
  public void testCanReadIntoBufferLargerThanDataSize() throws IOException {
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
  public void testCanReadBytesInBulkWithLimit() throws IOException {
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
  public void testCanReadBytesInBulkWithOffset() throws IOException {
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
  public void testCanReadBytesInBulkWhenSomeButNotAllBytesAreInBuffer() throws IOException {
    stream.read();
    byte[] buffer = new byte[BUFFER_SIZE];
    assertEquals(BUFFER_SIZE, stream.read(buffer, 0, BUFFER_SIZE));
    for (int i = 1; i < BUFFER_SIZE + 1; i++) {
      assertEquals(i, buffer[i - 1]);
    }
  }

  @Test
  public void testCanSkipBytes() throws IOException {
    int toSkip = data.length / 2;
    assertEquals(toSkip, stream.skip(toSkip));
    for (int i = toSkip; i < data.length; i++) {
      assertEquals(i, stream.read());
    }
    assertEquals(-1, stream.read());
  }

  @Test
  public void testSkipReturnsZeroIfSkipByteCountIsZero() throws IOException {
    assertEquals(0, stream.skip(0));
    assertEquals(0, stream.read());
  }

  @Test
  public void testSkipReturnsZeroIfSkipByteCountIsNegative() throws IOException {
    assertEquals(0, stream.skip(-13));
    assertEquals(0, stream.read());
  }

  @Test
  public void testCloseClosesWrappedStream() throws IOException {
    InputStream wrapped = mock(InputStream.class);
    stream = new RecyclableBufferedInputStream(wrapped, byteArrayPool);
    stream.close();
    verify(wrapped).close();
  }

  @Test
  public void testCanSafelyBeClosedMultipleTimes() throws IOException {
    InputStream wrapped = mock(InputStream.class);
    stream = new RecyclableBufferedInputStream(wrapped, byteArrayPool);
    stream.close();
    stream.close();
    stream.close();

    verify(wrapped, times(1)).close();
  }

  @Test
  public void testCanMarkAndReset() throws IOException {
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
  public void testCanResetRepeatedlyAfterMarking() throws IOException {
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
  public void testCanMarkInMiddleOfBufferAndStillReadUpToBufferLengthBeforeResetting()
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
  public void testAvailableReturnsWrappedAvailableIfNoBytesRead() throws IOException {
    assertEquals(DATA_SIZE, stream.available());
  }

  @Test
  public void testAvailableIncludesDataInBufferAndWrappedAvailableIfBytesRead() throws IOException {
    stream.read();
    assertEquals(DATA_SIZE - 1, stream.available());
  }

  @Test(expected = IOException.class)
  public void testCloseThrowsIfWrappedStreamThrowsOnClose() throws IOException {
    InputStream wrapped = mock(InputStream.class);
    doThrow(new IOException()).when(wrapped).close();
    stream = new RecyclableBufferedInputStream(wrapped, byteArrayPool);
    stream.close();
  }

  @Test(expected = IOException.class)
  public void testAvailableThrowsIfStreamIsClosed() throws IOException {
    stream.close();
    stream.available();
  }

  @Test(expected = IOException.class)
  public void testReadThrowsIfStreamIsClosed() throws IOException {
    stream.close();
    stream.read();
  }

  @Test(expected = IOException.class)
  public void testReadBulkThrowsIfStreamIsClosed() throws IOException {
    stream.close();
    stream.read(new byte[1], 0, 1);
  }

  @Test(expected = IOException.class)
  public void testResetThrowsIfStreamIsClosed() throws IOException {
    stream.close();
    stream.reset();
  }

  @Test(expected = IOException.class)
  public void testSkipThrowsIfStreamIsClosed() throws IOException {
    stream.close();
    stream.skip(10);
  }

  @Test(expected = RecyclableBufferedInputStream.InvalidMarkException.class)
  public void testResetThrowsIfMarkNotSet() throws IOException {
    stream.reset();
  }

  @Test(expected = RecyclableBufferedInputStream.InvalidMarkException.class)
  public void testResetThrowsIfMarkIsInvalid() throws IOException {
    stream.mark(1);
    stream.read(new byte[BUFFER_SIZE], 0, BUFFER_SIZE);
    stream.read();
    stream.reset();
  }
}
