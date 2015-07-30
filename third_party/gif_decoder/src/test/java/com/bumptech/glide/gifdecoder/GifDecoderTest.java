package com.bumptech.glide.gifdecoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.testutil.TestUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.io.IOException;

/**
 * Tests for {@link com.bumptech.glide.gifdecoder.GifDecoder}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class GifDecoderTest {

  private MockProvider provider;

  @Before
  public void setUp() {
    provider = new MockProvider();
  }

  @Test
  public void testCanDecodeFramesFromTestGif() throws IOException {
    byte[] data = TestUtil.resourceToBytes(getClass(), "partial_gif_decode.gif");
    GifHeaderParser headerParser = new GifHeaderParser();
    headerParser.setData(data);
    GifHeader header = headerParser.parseHeader();
    GifDecoder decoder = new GifDecoder(provider);
    decoder.setData(header, data);
    decoder.advance();
    Bitmap bitmap = decoder.getNextFrame();
    assertNotNull(bitmap);
    assertEquals(GifDecoder.STATUS_OK, decoder.getStatus());
  }

  @Test
  public void testFrameIndexStartsAtNegativeOne() {
    GifHeader gifheader = new GifHeader();
    gifheader.frameCount = 4;
    byte[] data = new byte[0];
    GifDecoder decoder = new GifDecoder(provider);
    decoder.setData(gifheader, data);
    assertEquals(-1, decoder.getCurrentFrameIndex());
  }

  @Test
  public void testAdvanceIncrementsFrameIndex() {
    GifHeader gifheader = new GifHeader();
    gifheader.frameCount = 4;
    byte[] data = new byte[0];
    GifDecoder decoder = new GifDecoder(provider);
    decoder.setData(gifheader, data);
    decoder.advance();
    assertEquals(0, decoder.getCurrentFrameIndex());
  }

  @Test
  public void testAdvanceWrapsIndexBackToZero() {
    GifHeader gifheader = new GifHeader();
    gifheader.frameCount = 2;
    byte[] data = new byte[0];
    GifDecoder decoder = new GifDecoder(provider);
    decoder.setData(gifheader, data);
    decoder.advance();
    decoder.advance();
    decoder.advance();
    assertEquals(0, decoder.getCurrentFrameIndex());
  }

  @Test
  public void testSettingDataResetsFramePointer() {
    GifHeader gifheader = new GifHeader();
    gifheader.frameCount = 4;
    byte[] data = new byte[0];
    GifDecoder decoder = new GifDecoder(provider);
    decoder.setData(gifheader, data);
    decoder.advance();
    decoder.advance();
    assertEquals(1, decoder.getCurrentFrameIndex());

    decoder.setData(gifheader, data);
    assertEquals(-1, decoder.getCurrentFrameIndex());
  }

  private static class MockProvider implements GifDecoder.BitmapProvider {

    @NonNull
    @Override
    public Bitmap obtain(int width, int height, Bitmap.Config config) {
      Bitmap result = Bitmap.createBitmap(width, height, config);
      Shadows.shadowOf(result).setMutable(true);
      return result;
    }

    @Override
    public void release(Bitmap bitmap) {
      // Do nothing.
    }

    @Override
    public byte[] obtainByteArray(int size) {
      return new byte[size];
    }

    @Override
    public void release(byte[] bytes) {
      // Do nothing.
    }

  }
}
