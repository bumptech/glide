package com.bumptech.glide.load.resource.bytes;

import androidx.annotation.NonNull;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.ByteBufferUtil;
import java.nio.ByteBuffer;

/** Returns the bytes in a {@link ByteBuffer} as {@link SourceBytes} without decoding them. */
public final class ByteBufferSourceBytesDecoder
    implements ResourceDecoder<ByteBuffer, SourceBytes> {

  @Override
  public boolean handles(@NonNull ByteBuffer source, @NonNull Options options) {
    return true;
  }

  @NonNull
  @Override
  public Resource<SourceBytes> decode(
      @NonNull ByteBuffer source, int width, int height, @NonNull Options options) {
    return new SourceBytesResource(new SourceBytes(ByteBufferUtil.toBytes(source)));
  }
}
