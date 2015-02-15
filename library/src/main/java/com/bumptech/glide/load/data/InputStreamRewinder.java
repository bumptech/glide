package com.bumptech.glide.load.data;

import com.bumptech.glide.load.resource.bitmap.RecyclableBufferedInputStream;
import com.bumptech.glide.util.ByteArrayPool;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation for {@link InputStream}s that rewinds streams by wrapping them in a buffered
 * stream.
 */
public final class InputStreamRewinder implements DataRewinder<InputStream> {
  // 5mb.
  private static final int MARK_LIMIT = 5 * 1024 * 1024;

  private final RecyclableBufferedInputStream bufferedStream;
  private final byte[] buffer;

  InputStreamRewinder(InputStream is) {
    ByteArrayPool byteArrayPool = ByteArrayPool.get();
    buffer = byteArrayPool.getBytes();
    bufferedStream = new RecyclableBufferedInputStream(is, buffer);
    bufferedStream.mark(MARK_LIMIT);
  }

  @Override
  public InputStream rewindAndGet() throws IOException {
    bufferedStream.reset();
    return bufferedStream;
  }

  @Override
  public void cleanup() {
    ByteArrayPool.get().releaseBytes(buffer);
  }

  /**
   * Factory for producing {@link com.bumptech.glide.load.data.InputStreamRewinder}s from {@link
   * java.io.InputStream}s.
   */
  public static class Factory implements DataRewinder.Factory<InputStream> {

    @Override
    public DataRewinder<InputStream> build(InputStream data) {
      return new InputStreamRewinder(data);
    }

    @Override
    public Class getDataClass() {
      return InputStream.class;
    }
  }

}
