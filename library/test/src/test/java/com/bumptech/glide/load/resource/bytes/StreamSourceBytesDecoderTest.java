package com.bumptech.glide.load.resource.bytes;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class StreamSourceBytesDecoderTest {
  private final StreamSourceBytesDecoder decoder = new StreamSourceBytesDecoder(new LruArrayPool());

  @Test
  public void handles_returnsTrue() {
    assertThat(decoder.handles(new ByteArrayInputStream(new byte[] {1, 2}), new Options()))
        .isTrue();
  }

  @Test
  public void decode_returnsAllBytesUnchanged() throws IOException {
    // Larger than the buffers used to read the stream.
    byte[] data = new byte[100_000];
    new Random(/* seed= */ 1).nextBytes(data);

    Resource<SourceBytes> resource =
        decoder.decode(
            new ByteArrayInputStream(data), /* width= */ 10, /* height= */ 10, new Options());

    assertThat(resource.get().getBytes()).isEqualTo(data);
    assertThat(resource.getSize()).isEqualTo(data.length);
  }

  @Test
  public void decode_withEmptyStream_returnsEmptyBytes() throws IOException {
    Resource<SourceBytes> resource =
        decoder.decode(
            new ByteArrayInputStream(new byte[0]),
            /* width= */ 10,
            /* height= */ 10,
            new Options());

    assertThat(resource.get().getBytes()).isEmpty();
  }
}
