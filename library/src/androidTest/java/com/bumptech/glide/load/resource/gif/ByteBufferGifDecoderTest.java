package com.bumptech.glide.load.resource.gif;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruByteArrayPool;
import com.bumptech.glide.tests.GlideShadowLooper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.nio.ByteBuffer;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18, shadows = GlideShadowLooper.class)
public class ByteBufferGifDecoderTest {
  private static final byte[] GIF_HEADER = new byte[] { 0x47, 0x49, 0x46 };

  private ByteBufferGifDecoder decoder;
  private GifHeaderParser parser;
  private ByteBufferGifDecoder.GifHeaderParserPool parserPool;
  private ByteBufferGifDecoder.GifDecoderPool decoderPool;
  private GifDecoder gifDecoder;
  private GifHeader gifHeader;
  private Options options;

  @Before
  public void setUp() {
    BitmapPool bitmapPool = mock(BitmapPool.class);

    gifHeader = Mockito.spy(new GifHeader());
    parser = mock(GifHeaderParser.class);
    when(parser.parseHeader()).thenReturn(gifHeader);
    parserPool = mock(ByteBufferGifDecoder.GifHeaderParserPool.class);
    when(parserPool.obtain(any(ByteBuffer.class))).thenReturn(parser);

    gifDecoder = mock(GifDecoder.class);
    decoderPool = mock(ByteBufferGifDecoder.GifDecoderPool.class);
    when(decoderPool.obtain(any(GifDecoder.BitmapProvider.class))).thenReturn(gifDecoder);

    options = new Options();
    decoder = new ByteBufferGifDecoder(RuntimeEnvironment.application, bitmapPool,
        new LruByteArrayPool(), parserPool, decoderPool);
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
  public void testDecodesFirstFrameAndReturnsGifDecoderToPool() {
    when(gifHeader.getNumFrames()).thenReturn(1);
    when(gifHeader.getStatus()).thenReturn(GifDecoder.STATUS_OK);
    when(gifDecoder.getNextFrame())
        .thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));

    ByteBuffer data = ByteBuffer.allocate(10);
    decoder.decode(data, 100, 100, options);

    InOrder order = inOrder(decoderPool, gifDecoder);
    order.verify(decoderPool).obtain(any(GifDecoder.BitmapProvider.class));
    order.verify(gifDecoder).setData(eq(gifHeader), eq(data));
    order.verify(gifDecoder).advance();
    order.verify(gifDecoder).getNextFrame();
    order.verify(decoderPool).release(eq(gifDecoder));
  }

  @Test
  public void testReturnsGifDecoderToPoolWhenDecoderThrows() {
    when(gifHeader.getNumFrames()).thenReturn(1);
    when(gifHeader.getStatus()).thenReturn(GifDecoder.STATUS_OK);
    when(gifDecoder.getNextFrame()).thenThrow(new RuntimeException("test"));
    try {
      decoder.decode(ByteBuffer.allocate(10), 100, 100, options);
      fail("Failed to receive expected exception");
    } catch (RuntimeException e) {
      // Expected.
    }

    verify(decoderPool).release(eq(gifDecoder));
  }

  @Test
  public void testReturnsNullIfGifDecoderFailsToDecodeFirstFrame() {
    when(gifHeader.getNumFrames()).thenReturn(1);
    when(gifHeader.getStatus()).thenReturn(GifDecoder.STATUS_OK);
    when(gifDecoder.getNextFrame()).thenReturn(null);

    assertNull(decoder.decode(ByteBuffer.allocate(10), 100, 100, options));
  }

  @Test
  public void testReturnsGifDecoderToPoolWhenGifDecoderReturnsNullFirstFrame() {
    when(gifHeader.getNumFrames()).thenReturn(1);
    when(gifHeader.getStatus()).thenReturn(GifDecoder.STATUS_OK);
    when(gifDecoder.getNextFrame()).thenReturn(null);

    decoder.decode(ByteBuffer.allocate(10), 100, 100, options);

    verify(decoderPool).release(eq(gifDecoder));
  }

  @Test
  public void testCanObtainNonNullDecoderFromPool() {
    GifDecoder.BitmapProvider provider = mock(GifDecoder.BitmapProvider.class);
    ByteBufferGifDecoder.GifDecoderPool pool = new ByteBufferGifDecoder.GifDecoderPool();
    assertNotNull(pool.obtain(provider));
  }

  @Test
  public void testCanPutAndObtainDecoderFromPool() {
    ByteBufferGifDecoder.GifDecoderPool pool = new ByteBufferGifDecoder.GifDecoderPool();
    pool.release(gifDecoder);
    GifDecoder fromPool = pool.obtain(mock(GifDecoder.BitmapProvider.class));
    assertEquals(gifDecoder, fromPool);
  }

  @Test
  public void testDecoderPoolClearsDecoders() {
    ByteBufferGifDecoder.GifDecoderPool pool = new ByteBufferGifDecoder.GifDecoderPool();
    pool.release(gifDecoder);
    verify(gifDecoder).clear();
  }
}
