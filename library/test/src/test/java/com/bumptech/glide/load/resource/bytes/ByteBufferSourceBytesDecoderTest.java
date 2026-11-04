package com.bumptech.glide.load.resource.bytes;

import static com.google.common.truth.Truth.assertThat;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import java.nio.ByteBuffer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ByteBufferSourceBytesDecoderTest {
  private final ByteBufferSourceBytesDecoder decoder = new ByteBufferSourceBytesDecoder();

  @Test
  public void handles_returnsTrue() {
    assertThat(decoder.handles(ByteBuffer.wrap(new byte[] {1, 2}), new Options())).isTrue();
  }

  @Test
  public void decode_withHeapBuffer_returnsBytesUnchanged() {
    byte[] data = new byte[] {1, 2, 3, 4};

    Resource<SourceBytes> resource =
        decoder.decode(ByteBuffer.wrap(data), /* width= */ 10, /* height= */ 10, new Options());

    assertThat(resource.get().getBytes()).isEqualTo(data);
    assertThat(resource.getSize()).isEqualTo(data.length);
    assertThat(resource.getResourceClass()).isEqualTo(SourceBytes.class);
  }

  @Test
  public void decode_withDirectBufferAtEnd_returnsAllBytes() {
    byte[] data = new byte[] {5, 6, 7, 8, 9};
    ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
    buffer.put(data);

    Resource<SourceBytes> resource =
        decoder.decode(buffer, /* width= */ 10, /* height= */ 10, new Options());

    assertThat(resource.get().getBytes()).isEqualTo(data);
  }
}
