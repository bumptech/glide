package com.bumptech.glide.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bumptech.glide.load.resource.bitmap.RecyclableBufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ExceptionCatchingInputStreamTest {

  private RecyclableBufferedInputStream wrapped;
  private ExceptionCatchingInputStream is;

  @Before
  public void setUp() throws Exception {
    wrapped = mock(RecyclableBufferedInputStream.class);
    is = new ExceptionCatchingInputStream();
    is.setInputStream(wrapped);
  }

  @After
  public void tearDown() {
    ExceptionCatchingInputStream.clearQueue();
  }

  @Test
  public void testReturnsWrappedAvailable() throws IOException {
    when(wrapped.available()).thenReturn(25);
    assertEquals(25, is.available());
  }

  @Test
  public void testCallsCloseOnWrapped() throws IOException {
    is.close();
    verify(wrapped).close();
  }

  @Test
  public void testCallsMarkOnWrapped() {
    int toMark = 50;
    is.mark(toMark);
    verify(wrapped).mark(eq(toMark));
  }

  @Test
  public void testReturnsWrappedMarkSupported() {
    when(wrapped.markSupported()).thenReturn(true);
    assertTrue(is.markSupported());
  }

  @Test
  public void testCallsReadByteArrayOnWrapped() throws IOException {
    byte[] buffer = new byte[100];
    when(wrapped.read(eq(buffer))).thenReturn(buffer.length);
    assertEquals(buffer.length, is.read(buffer));
  }

  @Test
  public void testCallsReadArrayWithOffsetAndCountOnWrapped() throws IOException {
    int offset = 5;
    int count = 100;
    byte[] buffer = new byte[105];

    when(wrapped.read(eq(buffer), eq(offset), eq(count))).thenReturn(count);
    assertEquals(count, is.read(buffer, offset, count));
  }

  @Test
  public void testCallsReadOnWrapped() throws IOException {
    when(wrapped.read()).thenReturn(1);
    assertEquals(1, is.read());
  }

  @Test
  public void testCallsResetOnWrapped() throws IOException {
    is.reset();
    verify(wrapped).reset();
  }

  @Test
  public void testCallsSkipOnWrapped() throws IOException {
    long toSkip = 67;
    long expected = 55;
    when(wrapped.skip(eq(toSkip))).thenReturn(expected);
    assertEquals(expected, is.skip(toSkip));
  }

  @Test
  public void testCatchesExceptionOnRead() throws IOException {
    IOException expected = new SocketTimeoutException();
    when(wrapped.read()).thenThrow(expected);
    int read = is.read();

    assertEquals(-1, read);
    assertEquals(expected, is.getException());
  }

  @Test
  public void testCatchesExceptionOnReadBuffer() throws IOException {
    IOException exception = new SocketTimeoutException();
    when(wrapped.read(any(byte[].class))).thenThrow(exception);

    int read = is.read(new byte[0]);
    assertEquals(-1, read);
    assertEquals(exception, is.getException());
  }

  @Test
  public void testCatchesExceptionOnReadBufferWithOffsetAndCount() throws IOException {
    IOException exception = new SocketTimeoutException();
    when(wrapped.read(any(byte[].class), anyInt(), anyInt())).thenThrow(exception);

    int read = is.read(new byte[0], 10, 100);
    assertEquals(-1, read);
    assertEquals(exception, is.getException());
  }

  @Test
  public void testCatchesExceptionOnSkip() throws IOException {
    IOException exception = new SocketTimeoutException();
    when(wrapped.skip(anyLong())).thenThrow(exception);

    long skipped = is.skip(100);
    assertEquals(0, skipped);
    assertEquals(exception, is.getException());
  }

  @Test
  public void testExceptionIsNotSetInitially() {
    assertNull(is.getException());
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void testResetsExceptionToNullOnRelease() throws IOException {
    IOException exception = new SocketTimeoutException();
    when(wrapped.read()).thenThrow(exception);
    is.read();
    is.release();
    assertNull(is.getException());
  }

  @Test
  public void testCanReleaseAnObtainFromPool() {
    is.release();
    InputStream fromPool = ExceptionCatchingInputStream.obtain(wrapped);
    assertEquals(is, fromPool);
  }

  @Test
  public void testCanObtainNewStreamFromPool() throws IOException {
    InputStream fromPool = ExceptionCatchingInputStream.obtain(wrapped);
    when(wrapped.read()).thenReturn(1);
    int read = fromPool.read();
    assertEquals(1, read);
  }
}
