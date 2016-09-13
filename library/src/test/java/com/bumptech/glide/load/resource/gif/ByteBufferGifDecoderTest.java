package com.bumptech.glide.load.resource.gif;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.tests.GlideShadowLooper;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18, shadows = GlideShadowLooper.class)
public class ByteBufferGifDecoderTest {
  private static final byte[] GIF_HEADER = new byte[] { 0x47, 0x49, 0x46 };
  static final int ARRAY_POOL_SIZE_BYTES = 4 * 1024 * 1024;

  private ByteBufferGifDecoder decoder;
  private GifHeader gifHeader;
  private Options options;

  @Mock BitmapPool bitmapPool;
  @Mock GifHeaderParser parser;
  @Mock GifDecoder gifDecoder;
  @Mock ByteBufferGifDecoder.GifHeaderParserPool parserPool;
  @Mock ByteBufferGifDecoder.GifDecoderFactory decoderFactory;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    gifHeader = Mockito.spy(new GifHeader());
    when(parser.parseHeader()).thenReturn(gifHeader);
    when(parserPool.obtain(isA(ByteBuffer.class))).thenReturn(parser);

    when(decoderFactory.build(isA(GifDecoder.BitmapProvider.class),
        eq(gifHeader), isA(ByteBuffer.class), anyInt()))
        .thenReturn(gifDecoder);

    options = new Options();
    decoder =
        new ByteBufferGifDecoder(
            RuntimeEnvironment.application,
            bitmapPool,
            new LruArrayPool(ARRAY_POOL_SIZE_BYTES),
            parserPool,
            decoderFactory);
  }

  @Test
  public void testDoesNotHandleStreamIfEnabledButNotAGif() throws IOException {
    assertThat(decoder.handles(ByteBuffer.allocate(0), options)).isFalse();
  }

  @Test
  public void testHandlesStreamIfContainsGifHeaderAndDisabledIsNotSet() throws IOException {
    assertThat(decoder.handles(ByteBuffer.wrap(GIF_HEADER), options)).isTrue();
  }

  @Test
  public void testHandlesStreamIfContainsGifHeaderAndDisabledIsFalse() throws IOException {
    options.set(ByteBufferGifDecoder.DISABLE_ANIMATION, false);
    assertThat(decoder.handles(ByteBuffer.wrap(GIF_HEADER), options)).isTrue();
  }

  @Test
  public void testDoesNotHandleStreamIfDisabled() throws IOException {
    options.set(ByteBufferGifDecoder.DISABLE_ANIMATION, true);
    assertThat(decoder.handles(ByteBuffer.wrap(GIF_HEADER), options)).isFalse();
  }

  @Test
  public void testReturnsNullIfParsedHeaderHasZeroFrames() throws IOException {
    when(gifHeader.getNumFrames()).thenReturn(0);

    assertNull(decoder.decode(ByteBuffer.allocate(10), 100, 100, options));
  }

  @Test
  public void testReturnsNullIfParsedHeaderHasFormatError() {
    when(gifHeader.getStatus()).thenReturn(GifDecoder.STATUS_FORMAT_ERROR);

    assertNull(decoder.decode(ByteBuffer.allocate(10), 100, 100, options));
  }

  @Test
  public void testReturnsNullIfParsedHeaderHasOpenError() {
    when(gifHeader.getStatus()).thenReturn(GifDecoder.STATUS_OPEN_ERROR);

    assertNull(decoder.decode(ByteBuffer.allocate(10), 100, 100, options));
  }

  @Test
  public void testReturnsParserToPool() throws IOException {
    decoder.decode(ByteBuffer.allocate(10), 100, 100, options);
    verify(parserPool).release(eq(parser));
  }

  @Test
  public void testReturnsParserToPoolWhenParserThrows() {
    when(parser.parseHeader()).thenThrow(new RuntimeException("Test"));
    try {
      decoder.decode(ByteBuffer.allocate(10), 100, 100, options);
      fail("Failed to receive expected exception");
    } catch (RuntimeException e) {
      // Expected.
    }

    verify(parserPool).release(eq(parser));
  }

  @Test
  public void testReturnsNullIfGifDecoderFailsToDecodeFirstFrame() {
    when(gifHeader.getNumFrames()).thenReturn(1);
    when(gifHeader.getStatus()).thenReturn(GifDecoder.STATUS_OK);
    when(gifDecoder.getNextFrame()).thenReturn(null);

    assertNull(decoder.decode(ByteBuffer.allocate(10), 100, 100, options));
  }
}
