package com.bumptech.glide.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ExceptionPassthroughInputStreamTest {

  private final InputStream validInputStream =
      new ByteArrayInputStream(
          new byte[] {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
          });
  private final InputStream throwingInputStream = new ExceptionThrowingInputStream();
  private ExceptionPassthroughInputStream validWrapper;
  private ExceptionPassthroughInputStream throwingWrapper;

  @Before
  public void setUp() throws Exception {
    validWrapper = new ExceptionPassthroughInputStream();
    validWrapper.setInputStream(validInputStream);
    throwingWrapper = new ExceptionPassthroughInputStream();
    throwingWrapper.setInputStream(throwingInputStream);
  }

  @After
  public void tearDown() {
    ExceptionPassthroughInputStream.clearQueue();
  }

  @Test
  public void testReturnsWrappedAvailable() throws IOException {
    assertEquals(validInputStream.available(), validWrapper.available());
  }

  @Test
  public void testCallsCloseOnWrapped() throws IOException {
    ExceptionPassthroughInputStream wrapper = new ExceptionPassthroughInputStream();
    final AtomicBoolean isClosed = new AtomicBoolean();
    wrapper.setInputStream(
        new InputStream() {
          @Override
          public int read() {
            return 0;
          }

          @Override
          public void close() throws IOException {
            super.close();
            isClosed.set(true);
          }
        });
    wrapper.close();
    assertThat(isClosed.get()).isTrue();
  }

  @Test
  public void testCallsMarkOnWrapped() throws IOException {
    int toMark = 5;
    validWrapper.mark(toMark);
    assertThat(validWrapper.read(new byte[5], 0, 5)).isEqualTo(5);
    validInputStream.reset();
    assertThat(validInputStream.read()).isEqualTo(0);
  }

  @Test
  public void testReturnsWrappedMarkSupported() {
    assertTrue(validWrapper.markSupported());
  }

  @Test
  public void testCallsReadByteArrayOnWrapped() throws IOException {
    byte[] buffer = new byte[8];
    assertEquals(buffer.length, validWrapper.read(buffer));
  }

  @Test
  public void testCallsReadArrayWithOffsetAndCountOnWrapped() throws IOException {
    int offset = 1;
    int count = 4;
    byte[] buffer = new byte[5];

    assertEquals(count, validWrapper.read(buffer, offset, count));
  }

  @Test
  public void testCallsReadOnWrapped() throws IOException {
    assertEquals(0, validWrapper.read());
    assertEquals(1, validWrapper.read());
    assertEquals(2, validWrapper.read());
  }

  @Test
  public void testCallsResetOnWrapped() throws IOException {
    validWrapper.mark(5);
    assertThat(validWrapper.read()).isEqualTo(0);
    assertThat(validWrapper.read()).isEqualTo(1);
    validWrapper.reset();
    assertThat(validWrapper.read()).isEqualTo(0);
  }

  @Test
  public void testCallsSkipOnWrapped() throws IOException {
    int toSkip = 5;
    assertThat(validWrapper.skip(toSkip)).isEqualTo(toSkip);
    assertThat(validWrapper.read()).isEqualTo(5);
  }

  @Test
  public void testCatchesExceptionOnRead() {
    SocketTimeoutException expected =
        assertThrows(
            SocketTimeoutException.class,
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                throwingWrapper.read();
              }
            });
    assertEquals(expected, throwingWrapper.getException());
  }

  @Test
  public void testCatchesExceptionOnReadBuffer() {
    SocketTimeoutException exception =
        assertThrows(
            SocketTimeoutException.class,
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                throwingWrapper.read(new byte[1]);
              }
            });
    assertEquals(exception, throwingWrapper.getException());
  }

  @Test
  public void testCatchesExceptionOnReadBufferWithOffsetAndCount() {
    SocketTimeoutException exception =
        assertThrows(
            SocketTimeoutException.class,
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                throwingWrapper.read(new byte[2], 1, 1);
              }
            });
    assertEquals(exception, throwingWrapper.getException());
  }

  @Test
  public void testCatchesExceptionOnSkip() {
    SocketTimeoutException exception =
        assertThrows(
            SocketTimeoutException.class,
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                throwingWrapper.skip(100);
              }
            });
    assertEquals(exception, throwingWrapper.getException());
  }

  @Test
  public void testExceptionIsNotSetInitially() {
    assertNull(validWrapper.getException());
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void testResetsExceptionToNullOnRelease() {
    assertThrows(
        SocketTimeoutException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            throwingWrapper.read();
          }
        });
    throwingWrapper.release();
    assertNull(validWrapper.getException());
  }

  @Test
  public void testCanReleaseAnObtainFromPool() {
    validWrapper.release();
    InputStream fromPool = ExceptionPassthroughInputStream.obtain(validInputStream);
    assertEquals(validWrapper, fromPool);
  }

  @Test
  public void testCanObtainNewStreamFromPool() throws IOException {
    InputStream fromPool = ExceptionPassthroughInputStream.obtain(validInputStream);
    int read = fromPool.read();
    assertEquals(0, read);
  }

  private static final class ExceptionThrowingInputStream extends InputStream {
    @Override
    public int read() throws IOException {
      throw new SocketTimeoutException();
    }
  }
}
