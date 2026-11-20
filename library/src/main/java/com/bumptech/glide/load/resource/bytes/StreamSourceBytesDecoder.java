package com.bumptech.glide.load.resource.bytes;

import androidx.annotation.NonNull;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.util.ByteBufferUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/** Reads all bytes of an {@link InputStream} into {@link SourceBytes} without decoding them. */
public final class StreamSourceBytesDecoder implements ResourceDecoder<InputStream, SourceBytes> {
  private final ArrayPool arrayPool;

  public StreamSourceBytesDecoder(@NonNull ArrayPool arrayPool) {
    this.arrayPool = arrayPool;
  }

  @Override
  public boolean handles(@NonNull InputStream source, @NonNull Options options) {
    return true;
  }

  @NonNull
  @Override
  public Resource<SourceBytes> decode(
      @NonNull InputStream source, int width, int height, @NonNull Options options)
      throws IOException {
    ByteBuffer buffer = ByteBufferUtil.fromStream(source, /* useHeapBuffer= */ true, arrayPool);
    return new SourceBytesResource(new SourceBytes(ByteBufferUtil.toBytes(buffer)));
  }
}
