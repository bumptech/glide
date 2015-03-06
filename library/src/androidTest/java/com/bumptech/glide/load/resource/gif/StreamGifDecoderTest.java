package com.bumptech.glide.load.resource.gif;

import static com.google.common.truth.Truth.assertThat;

import com.bumptech.glide.load.ResourceDecoder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class StreamGifDecoderTest {
  private static final byte[] GIF_HEADER = new byte[] { 0x47, 0x49, 0x46 };

  @Mock
  ResourceDecoder<ByteBuffer, GifDrawable> byteBufferDecoder;
  private StreamGifDecoder decoder;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    decoder = new StreamGifDecoder(byteBufferDecoder);
  }

  @Test
  public void testDoesNotHandleStreamIfEnabledButNotAGif() throws IOException {
    Map<String, Object> options = new HashMap<>();
    assertThat(decoder.handles(new ByteArrayInputStream(new byte[0]), options)).isFalse();
  }

  @Test
  public void testHandlesStreamIfContainsGifHeaderAndDisabledIsNotSet() throws IOException {
    Map<String, Object> options = new HashMap<>();
    assertThat(decoder.handles(new ByteArrayInputStream(GIF_HEADER), options)).isTrue();
  }

  @Test
  public void testHandlesStreamIfContainsGifHeaderAndDisabledIsFalse() throws IOException {
    Map<String, Object> options = new HashMap<>();
    options.put(StreamGifDecoder.KEY_DISABLE_ANIMATION, false);
    assertThat(decoder.handles(new ByteArrayInputStream(GIF_HEADER), options)).isTrue();
  }

  @Test
  public void testDoesNotHandleStreamIfDisabled() throws IOException {
    Map<String, Object> options = new HashMap<>();
    options.put(StreamGifDecoder.KEY_DISABLE_ANIMATION, true);
    assertThat(decoder.handles(new ByteArrayInputStream(GIF_HEADER), options)).isFalse();
  }
}
