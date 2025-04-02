package com.bumptech.glide.util;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class ContentLengthInputStreamTest {
  @Mock private InputStream wrapped;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testAvailable_withZeroReadsAndValidContentLength_returnsContentLength()
      throws IOException {
    int value = 123356;
    InputStream is = ContentLengthInputStream.obtain(wrapped, String.valueOf(value));

    assertThat(is.available()).isEqualTo(value);
  }

  @Test
  public void testAvailable_withNullContentLength_returnsWrappedAvailable() throws IOException {
    InputStream is = ContentLengthInputStream.obtain(wrapped, null /*contentLengthHeader*/);
    int expected = 1234;
    when(wrapped.available()).thenReturn(expected);

    assertThat(is.available()).isEqualTo(expected);
  }

  @Test
  public void testAvailable_withInvalidContentLength_returnsWrappedAvailable() throws IOException {
    InputStream is = ContentLengthInputStream.obtain(wrapped, "invalid_length");
    int expected = 567;
    when(wrapped.available()).thenReturn(expected);

    assertThat(is.available()).isEqualTo(expected);
  }

  @Test
  public void testAvailable_withRead_returnsContentLengthOffsetByRead() throws IOException {
    int contentLength = 999;
    InputStream is = ContentLengthInputStream.obtain(wrapped, String.valueOf(contentLength));
    when(wrapped.read()).thenReturn(1);

    assertThat(is.read()).isEqualTo(1);
    assertThat(is.available()).isEqualTo(contentLength - 1);
  }

  @Test
  public void testAvailable_handlesReadValueOfZero() throws IOException {
    int contentLength = 999;
    InputStream is = ContentLengthInputStream.obtain(wrapped, String.valueOf(contentLength));
    when(wrapped.read()).thenReturn(0);

    assertThat(is.read()).isEqualTo(0);
    assertThat(is.available()).isEqualTo(contentLength - 1);
  }

  @Test
  public void testAvailable_withReadBytes_returnsContentLengthOffsetByNumberOfBytes()
      throws IOException {
    int contentLength = 678;
    InputStream is = ContentLengthInputStream.obtain(wrapped, String.valueOf(contentLength));
    int read = 100;
    when(wrapped.read(any(byte[].class), anyInt(), anyInt())).thenReturn(read);

    assertThat(is.read(new byte[500], 0, 0)).isEqualTo(read);
    assertThat(is.available()).isEqualTo(contentLength - read);
  }

  @Test
  public void testRead_whenReturnsLessThanZeroWithoutReadingAllContent_throwsIOException()
      throws IOException {
    int contentLength = 1;
    InputStream is = ContentLengthInputStream.obtain(wrapped, String.valueOf(contentLength));
    when(wrapped.read()).thenReturn(-1);

    try {
      //noinspection ResultOfMethodCallIgnored
      is.read();
      fail("Failed to throw expected exception");
    } catch (IOException e) {
      // Expected.
    }
  }

  @Test
  public void testReadBytes_whenReturnsLessThanZeroWithoutReadingAllContent_throwsIOException()
      throws IOException {
    int contentLength = 2;
    InputStream is = ContentLengthInputStream.obtain(wrapped, String.valueOf(contentLength));
    when(wrapped.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);

    try {
      //noinspection ResultOfMethodCallIgnored
      is.read(new byte[10], 0, 0);
      fail("Failed to throw expected exception");
    } catch (IOException e) {
      // Expected.
    }
  }

  @Test
  public void testRead_whenReturnsLessThanZeroWithInvalidLength_doesNotThrow() throws IOException {
    InputStream is = ContentLengthInputStream.obtain(wrapped, "invalid_length");
    when(wrapped.read()).thenReturn(-1);
    //noinspection ResultOfMethodCallIgnored
    is.read();
  }

  @Test
  public void testReadBytes_whenReturnsLessThanZeroWithInvalidLength_doesNotThrow()
      throws IOException {
    InputStream is = ContentLengthInputStream.obtain(wrapped, "invalid_length");
    when(wrapped.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);
    //noinspection ResultOfMethodCallIgnored
    is.read(new byte[10], 0, 0);
  }

  @Test
  public void testRead_readWithZeroes_doesNotThrow() throws IOException {
    ByteArrayInputStream inner = new ByteArrayInputStream(new byte[] {0, 0, 0});
    InputStream is = ContentLengthInputStream.obtain(inner, 3);

    assertThat(is.read()).isEqualTo(0);
    assertThat(is.read()).isEqualTo(0);
    assertThat(is.read()).isEqualTo(0);
    assertThat(is.read()).isEqualTo(-1);
  }

  @Test
  public void testRead_readWithHighValues_doesNotThrow() throws IOException {
    ByteArrayInputStream inner =
        new ByteArrayInputStream(new byte[] {(byte) 0xF0, (byte) 0xA0, (byte) 0xFF});
    InputStream is = ContentLengthInputStream.obtain(inner, 3);

    assertThat(is.read()).isEqualTo(0xF0);
    assertThat(is.read()).isEqualTo(0xA0);
    assertThat(is.read()).isEqualTo(0xFF);
    assertThat(is.read()).isEqualTo(-1);
  }
}
