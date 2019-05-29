package com.bumptech.glide.load.resource.bytes;

import androidx.annotation.NonNull;
import com.bumptech.glide.load.data.DataRewinder;
import java.nio.ByteBuffer;

/** Rewinds {@link java.nio.ByteBuffer}s. */
public class ByteBufferRewinder implements DataRewinder<ByteBuffer> {
  private final ByteBuffer buffer;

  // Public API.
  @SuppressWarnings("WeakerAccess")
  public ByteBufferRewinder(ByteBuffer buffer) {
    this.buffer = buffer;
  }

  @NonNull
  @Override
  public ByteBuffer rewindAndGet() {
    buffer.position(0);
    return buffer;
  }

  @Override
  public void cleanup() {
    // Do nothing.
  }

  /** Factory for {@link com.bumptech.glide.load.resource.bytes.ByteBufferRewinder}. */
  public static class Factory implements DataRewinder.Factory<ByteBuffer> {

    @NonNull
    @Override
    public DataRewinder<ByteBuffer> build(ByteBuffer data) {
      return new ByteBufferRewinder(data);
    }

    @NonNull
    @Override
    public Class<ByteBuffer> getDataClass() {
      return ByteBuffer.class;
    }
  }
}
