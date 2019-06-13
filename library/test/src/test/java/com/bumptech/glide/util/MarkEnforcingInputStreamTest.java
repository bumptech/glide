package com.bumptech.glide.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MarkEnforcingInputStreamTest {
  // An arbitrary number > 0.
  private static final int MARK_LIMIT = 5;
  // Another arbitrary number > MARK_LIMIT.
  private static final int DATA_SIZE = MARK_LIMIT + 1;

  @Test
  public void testReturnsByte_whenReadsUpToMarkLimit_withMoreBytesAvailable() throws IOException {
    MarkEnforcingInputStream is =
        new MarkEnforcingInputStream(new ByteArrayInputStream(new byte[DATA_SIZE]));
    is.mark(MARK_LIMIT);

    for (int i = 0; i < MARK_LIMIT; i++) {
      assertThat(is.read()).isAtLeast(0);
    }
  }

  @Test
  public void testReturnsByte_whenReadsUpToMarkLimit_withNoMoreBytesAvailable() throws IOException {
    MarkEnforcingInputStream is =
        new MarkEnforcingInputStream(new ByteArrayInputStream(new byte[MARK_LIMIT]));

    for (int i = 0; i < MARK_LIMIT; i++) {
      assertThat(is.read()).isAtLeast(0);
    }
  }

  @Test
  public void testReturnsEndOfStream_whenReadsSingleBytePastMarkLimit() throws IOException {
    MarkEnforcingInputStream is =
        new MarkEnforcingInputStream(new ByteArrayInputStream(new byte[DATA_SIZE]));

    is.mark(MARK_LIMIT);
    for (int i = 0; i < MARK_LIMIT; i++) {
      assertThat(is.read()).isAtLeast(0);
    }

    assertEquals(-1, is.read());
  }

  @Test
  public void
      testOverridesByteCount_whenReadBufferLargerThanMarkLimit_withNonZeroBytesRemainingInMarkLimit()
          throws IOException {
    MarkEnforcingInputStream is =
        new MarkEnforcingInputStream(new ByteArrayInputStream(new byte[DATA_SIZE]));

    is.mark(MARK_LIMIT);
    byte[] buffer = new byte[DATA_SIZE];
    assertEquals(MARK_LIMIT, is.read(buffer));
  }

  @Test
  public void
      testReturnsEndOfStream_whenReadBufferLargerThanMarkLimit_withZeroBytesRemainingInMarkLimit()
          throws IOException {
    MarkEnforcingInputStream is =
        new MarkEnforcingInputStream(new ByteArrayInputStream(new byte[DATA_SIZE]));
    is.mark(MARK_LIMIT);

    byte[] buffer = new byte[MARK_LIMIT];
    assertEquals(MARK_LIMIT, is.read(buffer));
    assertEquals(-1, is.read(buffer));
  }

  @Test
  public void testDoesNotReadIntoBuffer_withZeroBytesRemainingInMarkLimit() throws IOException {
    byte[] expected = new byte[MARK_LIMIT];
    for (int i = 0; i < MARK_LIMIT; i++) {
      expected[i] = (byte) (i + 1);
    }
    byte[] buffer = new byte[MARK_LIMIT];
    System.arraycopy(expected, 0, buffer, 0, MARK_LIMIT);

    // All zeros.
    MarkEnforcingInputStream is =
        new MarkEnforcingInputStream(new ByteArrayInputStream(new byte[DATA_SIZE]));
    is.mark(MARK_LIMIT);
    for (int i = 0; i < MARK_LIMIT; i++) {
      assertThat(is.read()).isAtLeast(0);
    }

    assertEquals(-1, is.read(buffer));

    assertThat(buffer).isEqualTo(expected);
  }

  @Test
  public void testResetUnsetsLimit() throws IOException {
    MarkEnforcingInputStream is =
        new MarkEnforcingInputStream(new ByteArrayInputStream(new byte[DATA_SIZE]));
    is.mark(MARK_LIMIT);

    for (int i = 0; i < MARK_LIMIT; i++) {
      assertThat(is.read()).isAtLeast(0);
    }

    is.reset();

    for (int i = 0; i < DATA_SIZE; i++) {
      assertThat(is.read()).isAtLeast(0);
    }
  }

  @Test
  public void
      testOverridesByteCount_whenSkipCountLargerThanMarkLimit_withNonZeroBytesRemainingInMarkLimit()
          throws IOException {
    MarkEnforcingInputStream is =
        new MarkEnforcingInputStream(new ByteArrayInputStream(new byte[DATA_SIZE]));
    is.mark(MARK_LIMIT);

    assertEquals(MARK_LIMIT, is.skip(DATA_SIZE));
  }

  @Test
  public void testReturnsEndOfStream_whenSkipping_withZeroBytesRemainingInMarkLimit()
      throws IOException {
    MarkEnforcingInputStream is =
        new MarkEnforcingInputStream(new ByteArrayInputStream(new byte[DATA_SIZE]));
    is.mark(MARK_LIMIT);

    assertEquals(MARK_LIMIT, is.skip(DATA_SIZE));
    assertEquals(0, is.skip(1));
  }

  @Test
  public void testReturnsStreamAvailable_whenMarkIsNotSet() throws IOException {
    ByteArrayInputStream wrapped = new ByteArrayInputStream(new byte[MARK_LIMIT]);
    MarkEnforcingInputStream is = new MarkEnforcingInputStream(wrapped);

    assertEquals(wrapped.available(), is.available());
  }

  @Test
  public void testReturnsStreamAvailable_whenMarkIsSet_withMarkGreaterThanStreamAvailable()
      throws IOException {
    ByteArrayInputStream wrapped = new ByteArrayInputStream(new byte[MARK_LIMIT]);
    MarkEnforcingInputStream is = new MarkEnforcingInputStream(wrapped);
    is.mark(wrapped.available() + 1);

    assertEquals(wrapped.available(), is.available());
  }

  @Test
  public void testReturnsMarkLimitAsAvailable_whenMarkIsSet_withMarkLessThanStreamAvailable()
      throws IOException {
    ByteArrayInputStream wrapped = new ByteArrayInputStream(new byte[MARK_LIMIT]);
    MarkEnforcingInputStream is = new MarkEnforcingInputStream(wrapped);
    int expected = wrapped.available() - 1;
    is.mark(expected);

    assertEquals(expected, is.available());
  }
}
