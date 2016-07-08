package com.bumptech.glide.load.resource.bytes;

import com.bumptech.glide.load.data.DataRewinder;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Rewinds {@link java.nio.ByteBuffer}s.
 */
public class ByteBufferRewinder implements DataRewinder<ByteBuffer> {
  private final ByteBuffer buffer;

  public ByteBufferRewinder(ByteBuffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public ByteBuffer rewindAndGet() throws IOException {
    buffer.position(0);
    return buffer;
  }

  @Override
  public void cleanup() {
    // Do nothing.
  }

  /**
   * Factory for {@link com.bumptech.glide.load.resource.bytes.ByteBufferRewinder}.
   */
  public static class Factory implements DataRewinder.Factory<ByteBuffer> {

    @Override
    public DataRewinder<ByteBuffer> build(ByteBuffer data) {
      return new ByteBufferRewinder(data);
    }

    @Override
    public Class<ByteBuffer> getDataClass() {
      return ByteBuffer.class;
    }
  }
}
