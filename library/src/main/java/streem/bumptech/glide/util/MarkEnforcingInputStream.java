package com.bumptech.glide.util;

import androidx.annotation.NonNull;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Prevents {@link InputStream InputStreams} from overflowing their buffer by reading data past
 * their read limit.
 */
public class MarkEnforcingInputStream extends FilterInputStream {
  private static final int UNSET = Integer.MIN_VALUE;
  private static final int END_OF_STREAM = -1;

  private int availableBytes = UNSET;

  public MarkEnforcingInputStream(@NonNull InputStream in) {
    super(in);
  }

  @Override
  public synchronized void mark(int readLimit) {
    super.mark(readLimit);
    availableBytes = readLimit;
  }

  @Override
  public int read() throws IOException {
    if (getBytesToRead(1) == END_OF_STREAM) {
      return END_OF_STREAM;
    }

    int result = super.read();
    updateAvailableBytesAfterRead(1 /* bytesRead */);
    return result;
  }

  @Override
  public int read(@NonNull byte[] buffer, int byteOffset, int byteCount) throws IOException {
    int toRead = (int) getBytesToRead(byteCount);
    if (toRead == END_OF_STREAM) {
      return END_OF_STREAM;
    }

    int read = super.read(buffer, byteOffset, toRead);
    updateAvailableBytesAfterRead(read);
    return read;
  }

  @Override
  public synchronized void reset() throws IOException {
    super.reset();
    availableBytes = UNSET;
  }

  @Override
  public long skip(long byteCount) throws IOException {
    long toSkip = getBytesToRead(byteCount);
    if (toSkip == END_OF_STREAM) {
      return 0;
    }

    long read = super.skip(toSkip);
    updateAvailableBytesAfterRead(read);
    return read;
  }

  @Override
  public int available() throws IOException {
    return availableBytes == UNSET
        ? super.available()
        : Math.min(availableBytes, super.available());
  }

  private long getBytesToRead(long targetByteCount) {
    if (availableBytes == 0) {
      return END_OF_STREAM;
    } else if (availableBytes != UNSET && targetByteCount > availableBytes) {
      return availableBytes;
    } else {
      return targetByteCount;
    }
  }

  private void updateAvailableBytesAfterRead(long bytesRead) {
    if (availableBytes != UNSET && bytesRead != END_OF_STREAM) {
      // See https://errorprone.info/bugpattern/NarrowingCompoundAssignment.
      availableBytes = (int) (availableBytes - bytesRead);
    }
  }
}
