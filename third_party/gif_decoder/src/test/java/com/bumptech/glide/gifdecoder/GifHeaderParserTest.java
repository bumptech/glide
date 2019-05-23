package com.bumptech.glide.gifdecoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bumptech.glide.gifdecoder.test.GifBytesTestUtil;
import com.bumptech.glide.testutil.TestUtil;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link com.bumptech.glide.gifdecoder.GifHeaderParser}.
 */
@RunWith(JUnit4.class)
public class GifHeaderParserTest {
  private GifHeaderParser parser;

  @Before
  public void setUp() {
    parser = new GifHeaderParser();
  }

  @Test
  public void testReturnsHeaderWithFormatErrorIfDoesNotStartWithGifHeader() {
    parser.setData("wrong_header".getBytes());
    GifHeader result = parser.parseHeader();
    assertEquals(GifDecoder.STATUS_FORMAT_ERROR, result.status);
  }

  @Test
  public void testCanReadValidHeaderAndLSD() {
    final int width = 10;
    final int height = 20;
    ByteBuffer buffer =
        ByteBuffer.allocate(GifBytesTestUtil.HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
    GifBytesTestUtil.writeHeaderAndLsd(buffer, width, height, false, 0);

    parser.setData(buffer.array());
    GifHeader header = parser.parseHeader();
    assertEquals(width, header.width);
    assertEquals(height, header.height);
    assertFalse(header.gctFlag);
    // 2^(1+0) == 2^1 == 2.
    assertEquals(2, header.gctSize);
    assertEquals(0, header.bgIndex);
    assertEquals(0, header.pixelAspect);
  }

  @Test
  public void testCanParseHeaderOfTestImageWithoutGraphicalExtension() throws IOException {
    byte[] data =
        TestUtil.resourceToBytes(getClass(), "gif_without_graphical_control_extension.gif");
    parser.setData(data);
    GifHeader header = parser.parseHeader();
    assertEquals(1, header.frameCount);
    assertNotNull(header.frames.get(0));
    assertEquals(GifDecoder.STATUS_OK, header.status);
  }

  @Test
  public void testCanReadNetscapeIterationCountIfNetscapeIterationCountIsZero() throws IOException {
    byte[] data = TestUtil.resourceToBytes(getClass(), "gif_netscape_iteration_0.gif");
    parser.setData(data);
    GifHeader header = parser.parseHeader();
    assertEquals(GifHeader.NETSCAPE_LOOP_COUNT_FOREVER, header.loopCount);
  }

  @Test
  public void testCanReadNetscapeIterationCountIfNetscapeIterationCountIs_1() throws IOException {
    byte[] data = TestUtil.resourceToBytes(getClass(), "gif_netscape_iteration_1.gif");
    parser.setData(data);
    GifHeader header = parser.parseHeader();
    assertEquals(1, header.loopCount);
  }

  @Test
  public void testCanReadNetscapeIterationCountIfNetscapeIterationCountIs_0x0F()
      throws IOException {
    byte[] data = TestUtil.resourceToBytes(getClass(), "gif_netscape_iteration_255.gif");
    parser.setData(data);
    GifHeader header = parser.parseHeader();
    assertEquals(255, header.loopCount);
  }

  @Test
  public void testCanReadNetscapeIterationCountIfNetscapeIterationCountIs_0x10()
      throws IOException {
    byte[] data = TestUtil.resourceToBytes(getClass(), "gif_netscape_iteration_256.gif");
    parser.setData(data);
    GifHeader header = parser.parseHeader();
    assertEquals(256, header.loopCount);
  }

  @Test
  public void testCanReadNetscapeIterationCountIfNetscapeIterationCountIs_0xFF()
      throws IOException {
    byte[] data = TestUtil.resourceToBytes(getClass(), "gif_netscape_iteration_65535.gif");
    parser.setData(data);
    GifHeader header = parser.parseHeader();
    assertEquals(65535, header.loopCount);
  }

  @Test
  public void testLoopCountReturnsMinusOneWithoutNetscapeIterationCount()
          throws IOException {
    byte[] data = TestUtil.resourceToBytes(getClass(), "gif_without_netscape_iteration.gif");
    parser.setData(data);
    GifHeader header = parser.parseHeader();
    assertEquals(GifHeader.NETSCAPE_LOOP_COUNT_DOES_NOT_EXIST, header.loopCount);
  }

  @Test
  public void testCanReadImageDescriptorWithoutGraphicalExtension() {
    final int lzwMinCodeSize = 2;
    ByteBuffer buffer = ByteBuffer.allocate(
        GifBytesTestUtil.HEADER_LENGTH + GifBytesTestUtil.IMAGE_DESCRIPTOR_LENGTH + GifBytesTestUtil
            .getImageDataSize()).order(ByteOrder.LITTLE_ENDIAN);
    GifBytesTestUtil.writeHeaderAndLsd(buffer, 1, 1, false, 0);
    GifBytesTestUtil.writeImageDescriptor(buffer, 0, 0, 1, 1, false /*hasLct*/, 0);
    GifBytesTestUtil.writeFakeImageData(buffer, lzwMinCodeSize);

    parser.setData(buffer.array());
    GifHeader header = parser.parseHeader();
    assertEquals(1, header.width);
    assertEquals(1, header.height);
    assertEquals(1, header.frameCount);
    assertNotNull(header.frames.get(0));
  }

  private static ByteBuffer writeHeaderWithGceAndFrameDelay(short frameDelay) {
    final int lzwMinCodeSize = 2;
    ByteBuffer buffer = ByteBuffer.allocate(
        GifBytesTestUtil.HEADER_LENGTH + GifBytesTestUtil.GRAPHICS_CONTROL_EXTENSION_LENGTH
            + GifBytesTestUtil.IMAGE_DESCRIPTOR_LENGTH + GifBytesTestUtil
            .getImageDataSize()).order(ByteOrder.LITTLE_ENDIAN);
    GifBytesTestUtil.writeHeaderAndLsd(buffer, 1, 1, false, 0);
    GifBytesTestUtil.writeGraphicsControlExtension(buffer, frameDelay);
    GifBytesTestUtil.writeImageDescriptor(buffer, 0, 0, 1, 1, false /*hasLct*/, 0);
    GifBytesTestUtil.writeFakeImageData(buffer, lzwMinCodeSize);
    return buffer;
  }

  @Test
  public void testCanParseFrameDelay() {
    final short frameDelay = 50;
    ByteBuffer buffer = writeHeaderWithGceAndFrameDelay(frameDelay);

    parser.setData(buffer.array());
    GifHeader header = parser.parseHeader();
    GifFrame frame = header.frames.get(0);

    // Convert delay in 100ths of a second to ms.
    assertEquals(frameDelay * 10, frame.delay);
  }

  @Test
  public void testSetsDefaultFrameDelayIfFrameDelayIsZero() {
    ByteBuffer buffer = writeHeaderWithGceAndFrameDelay((short) 0);

    parser.setData(buffer.array());
    GifHeader header = parser.parseHeader();
    GifFrame frame = header.frames.get(0);

    // Convert delay in 100ths of a second to ms.
    assertEquals(GifHeaderParser.DEFAULT_FRAME_DELAY * 10, frame.delay);
  }

  @Test
  public void testSetsDefaultFrameDelayIfFrameDelayIsLessThanMinimum() {
    final short frameDelay = GifHeaderParser.MIN_FRAME_DELAY - 1;
    ByteBuffer buffer = writeHeaderWithGceAndFrameDelay(frameDelay);

    parser.setData(buffer.array());
    GifHeader header = parser.parseHeader();
    GifFrame frame = header.frames.get(0);

    // Convert delay in 100ths of a second to ms.
    assertEquals(GifHeaderParser.DEFAULT_FRAME_DELAY * 10, frame.delay);
  }

  @Test
  public void testObeysFrameDelayIfFrameDelayIsAtMinimum() {
    final short frameDelay = GifHeaderParser.MIN_FRAME_DELAY;
    ByteBuffer buffer = writeHeaderWithGceAndFrameDelay(frameDelay);

    parser.setData(buffer.array());
    GifHeader header = parser.parseHeader();
    GifFrame frame = header.frames.get(0);

    // Convert delay in 100ths of a second to ms.
    assertEquals(frameDelay * 10, frame.delay);
  }

  @Test
  public void testSetsFrameLocalColorTableToNullIfNoColorTable() {
    final int lzwMinCodeSize = 2;
    ByteBuffer buffer = ByteBuffer.allocate(
        GifBytesTestUtil.HEADER_LENGTH + GifBytesTestUtil.IMAGE_DESCRIPTOR_LENGTH + GifBytesTestUtil
            .getImageDataSize()).order(ByteOrder.LITTLE_ENDIAN);
    GifBytesTestUtil.writeHeaderAndLsd(buffer, 1, 1, false, 0);
    GifBytesTestUtil.writeImageDescriptor(buffer, 0, 0, 1, 1, false /*hasLct*/, 0);
    GifBytesTestUtil.writeFakeImageData(buffer, lzwMinCodeSize);

    parser.setData(buffer.array());
    GifHeader header = parser.parseHeader();
    assertEquals(1, header.width);
    assertEquals(1, header.height);
    assertEquals(1, header.frameCount);
    assertNotNull(header.frames.get(0));
    assertNull(header.frames.get(0).lct);
  }

  @Test
  public void testSetsFrameLocalColorTableIfHasColorTable() {
    final int lzwMinCodeSize = 2;
    final int numColors = 4;
    ByteBuffer buffer = ByteBuffer.allocate(
        GifBytesTestUtil.HEADER_LENGTH + GifBytesTestUtil.IMAGE_DESCRIPTOR_LENGTH + GifBytesTestUtil
            .getImageDataSize() + GifBytesTestUtil.getColorTableLength(numColors))
        .order(ByteOrder.LITTLE_ENDIAN);
    GifBytesTestUtil.writeHeaderAndLsd(buffer, 1, 1, false, 0);
    GifBytesTestUtil.writeImageDescriptor(buffer, 0, 0, 1, 1, true /*hasLct*/, numColors);
    GifBytesTestUtil.writeColorTable(buffer, numColors);
    GifBytesTestUtil.writeFakeImageData(buffer, 2);

    parser.setData(buffer.array());
    GifHeader header = parser.parseHeader();
    assertEquals(1, header.width);
    assertEquals(1, header.height);
    assertEquals(1, header.frameCount);
    assertNotNull(header.frames.get(0));

    GifFrame frame = header.frames.get(0);
    assertNotNull(frame.lct);
  }

  @Test
  public void testCanParseMultipleFrames() {
    final int lzwMinCodeSize = 2;
    final int expectedFrames = 3;

    final int frameSize = GifBytesTestUtil.IMAGE_DESCRIPTOR_LENGTH + GifBytesTestUtil
        .getImageDataSize();
    ByteBuffer buffer =
        ByteBuffer.allocate(GifBytesTestUtil.HEADER_LENGTH + expectedFrames * frameSize)
            .order(ByteOrder.LITTLE_ENDIAN);

    GifBytesTestUtil.writeHeaderAndLsd(buffer, 1, 1, false, 0);
    for (int i = 0; i < expectedFrames; i++) {
      GifBytesTestUtil.writeImageDescriptor(buffer, 0, 0, 1, 1, false /*hasLct*/, 0 /*numColors*/);
      GifBytesTestUtil.writeFakeImageData(buffer, 2);
    }

    parser.setData(buffer.array());
    GifHeader header = parser.parseHeader();
    assertEquals(expectedFrames, header.frameCount);
    assertEquals(expectedFrames, header.frames.size());
  }

  @Test
  public void testIsAnimatedMultipleFrames() {
    final int lzwMinCodeSize = 2;
    final int numFrames = 3;

    final int frameSize =
        GifBytesTestUtil.IMAGE_DESCRIPTOR_LENGTH
            + GifBytesTestUtil.getImageDataSize();
    ByteBuffer buffer =
        ByteBuffer.allocate(GifBytesTestUtil.HEADER_LENGTH + numFrames * frameSize)
            .order(ByteOrder.LITTLE_ENDIAN);

    GifBytesTestUtil.writeHeaderAndLsd(buffer, 1, 1, false, 0);
    for (int i = 0; i < numFrames; i++) {
      GifBytesTestUtil.writeImageDescriptor(buffer, 0, 0, 1, 1, false /*hasLct*/, 0 /*numColors*/);
      GifBytesTestUtil.writeFakeImageData(buffer, 2);
    }

    parser.setData(buffer.array());
    assertTrue(parser.isAnimated());
  }

  @Test
  public void testIsNotAnimatedOneFrame() {
    final int lzwMinCodeSize = 2;

    final int frameSize =
        GifBytesTestUtil.IMAGE_DESCRIPTOR_LENGTH
            + GifBytesTestUtil.getImageDataSize();

    ByteBuffer buffer =
        ByteBuffer.allocate(GifBytesTestUtil.HEADER_LENGTH + frameSize)
            .order(ByteOrder.LITTLE_ENDIAN);

    GifBytesTestUtil.writeHeaderAndLsd(buffer, 1, 1, false, 0);
    GifBytesTestUtil.writeImageDescriptor(buffer, 0, 0, 1, 1, false /*hasLct*/, 0 /*numColors*/);
    GifBytesTestUtil.writeFakeImageData(buffer, 2);

    parser.setData(buffer.array());
    assertFalse(parser.isAnimated());
  }


  @Test(expected = IllegalStateException.class)
  public void testThrowsIfParseHeaderCalledBeforeSetData() {
    GifHeaderParser parser = new GifHeaderParser();
    parser.parseHeader();
  }
}
