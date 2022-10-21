package com.bumptech.glide.gifdecoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import com.bumptech.glide.testutil.TestUtil;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

/** Tests for {@link com.bumptech.glide.gifdecoder.GifDecoder}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class GifDecoderTest {

  private MockProvider provider;

  @Before
  public void setUp() {
    provider = new MockProvider();
  }

  @Test
  public void testCorrectPixelsDecoded() throws IOException {
    byte[] data = TestUtil.resourceToBytes(getClass(), "white_black_row.gif");
    GifHeaderParser headerParser = new GifHeaderParser();
    headerParser.setData(data);
    GifHeader header = headerParser.parseHeader();
    GifDecoder decoder = new StandardGifDecoder(provider);
    decoder.setData(header, data);
    decoder.advance();
    Bitmap bitmap = decoder.getNextFrame();
    assertNotNull(bitmap);
    assertEquals(bitmap.getPixel(2, 0), bitmap.getPixel(0, 0));
    assertEquals(bitmap.getPixel(3, 0), bitmap.getPixel(1, 0));
  }

  @Test
  public void testCanDecodeFramesFromTestGif() throws IOException {
    byte[] data = TestUtil.resourceToBytes(getClass(), "partial_gif_decode.gif");
    GifHeaderParser headerParser = new GifHeaderParser();
    headerParser.setData(data);
    GifHeader header = headerParser.parseHeader();
    GifDecoder decoder = new StandardGifDecoder(provider);
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
    GifDecoder decoder = new StandardGifDecoder(provider);
    decoder.setData(gifheader, data);
    assertEquals(-1, decoder.getCurrentFrameIndex());
  }

  @Test
  public void testTotalIterationCountIsOneIfNetscapeLoopCountDoesntExist() {
    GifHeader gifheader = new GifHeader();
    gifheader.loopCount = GifHeader.NETSCAPE_LOOP_COUNT_DOES_NOT_EXIST;
    byte[] data = new byte[0];
    GifDecoder decoder = new StandardGifDecoder(provider);
    decoder.setData(gifheader, data);
    assertEquals(1, decoder.getTotalIterationCount());
  }

  @Test
  public void testTotalIterationCountIsForeverIfNetscapeLoopCountIsForever() {
    GifHeader gifheader = new GifHeader();
    gifheader.loopCount = GifHeader.NETSCAPE_LOOP_COUNT_FOREVER;
    byte[] data = new byte[0];
    GifDecoder decoder = new StandardGifDecoder(provider);
    decoder.setData(gifheader, data);
    assertEquals(GifDecoder.TOTAL_ITERATION_COUNT_FOREVER, decoder.getTotalIterationCount());
  }

  @Test
  public void testTotalIterationCountIsTwoIfNetscapeLoopCountIsOne() {
    GifHeader gifheader = new GifHeader();
    gifheader.loopCount = 1;
    byte[] data = new byte[0];
    GifDecoder decoder = new StandardGifDecoder(provider);
    decoder.setData(gifheader, data);
    assertEquals(2, decoder.getTotalIterationCount());
  }

  @Test
  public void testAdvanceIncrementsFrameIndex() {
    GifHeader gifheader = new GifHeader();
    gifheader.frameCount = 4;
    byte[] data = new byte[0];
    GifDecoder decoder = new StandardGifDecoder(provider);
    decoder.setData(gifheader, data);
    decoder.advance();
    assertEquals(0, decoder.getCurrentFrameIndex());
  }

  @Test
  public void testAdvanceWrapsIndexBackToZero() {
    GifHeader gifheader = new GifHeader();
    gifheader.frameCount = 2;
    byte[] data = new byte[0];
    GifDecoder decoder = new StandardGifDecoder(provider);
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
    GifDecoder decoder = new StandardGifDecoder(provider);
    decoder.setData(gifheader, data);
    decoder.advance();
    decoder.advance();
    assertEquals(1, decoder.getCurrentFrameIndex());

    decoder.setData(gifheader, data);
    assertEquals(-1, decoder.getCurrentFrameIndex());
  }

  @Test
  public void testFirstFrameMustClearBeforeDrawingWhenLastFrameIsDisposalBackground()
      throws IOException {
    byte[] data = TestUtil.resourceToBytes(getClass(), "transparent_disposal_background.gif");
    GifHeaderParser headerParser = new GifHeaderParser();
    headerParser.setData(data);
    GifHeader header = headerParser.parseHeader();
    GifDecoder decoder = new StandardGifDecoder(provider);
    decoder.setData(header, data);
    decoder.advance();
    Bitmap firstFrame = decoder.getNextFrame();
    decoder.advance();
    decoder.getNextFrame();
    decoder.advance();
    Bitmap firstFrameTwice = decoder.getNextFrame();
    assertTrue(firstFrame.sameAs(firstFrameTwice));
  }

  @Test
  public void testFirstFrameMustClearBeforeDrawingWhenLastFrameIsDisposalNone() throws IOException {
    byte[] data = TestUtil.resourceToBytes(getClass(), "transparent_disposal_none.gif");
    GifHeaderParser headerParser = new GifHeaderParser();
    headerParser.setData(data);
    GifHeader header = headerParser.parseHeader();
    GifDecoder decoder = new StandardGifDecoder(provider);
    decoder.setData(header, data);
    decoder.advance();
    Bitmap firstFrame = decoder.getNextFrame();
    decoder.advance();
    decoder.getNextFrame();
    decoder.advance();
    Bitmap firstFrameTwice = decoder.getNextFrame();
    assertTrue(firstFrame.sameAs(firstFrameTwice));
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
    public void release(@NonNull Bitmap bitmap) {
      // Do nothing.
    }

    @NonNull
    @Override
    public byte[] obtainByteArray(int size) {
      return new byte[size];
    }

    @Override
    public void release(@NonNull byte[] bytes) {
      // Do nothing.
    }

    @NonNull
    @Override
    public int[] obtainIntArray(int size) {
      return new int[size];
    }

    @Override
    public void release(@NonNull int[] array) {
      // Do Nothing
    }

  }
}
