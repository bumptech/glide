package com.bumptech.glide.integration.gifencoder;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.gifencoder.AnimatedGifEncoder;
import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.util.ByteBufferUtil;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Tests for {@link com.bumptech.glide.integration.gifencoder.ReEncodingGifResourceEncoder}. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class ReEncodingGifResourceEncoderTest {
  @Mock private Resource<GifDrawable> resource;
  @Mock private GifDecoder decoder;
  @Mock private GifHeaderParser parser;
  @Mock private AnimatedGifEncoder gifEncoder;
  @Mock private Resource<Bitmap> frameResource;
  @Mock private GifDrawable gifDrawable;
  @Mock private Transformation<Bitmap> frameTransformation;
  @Mock private Resource<Bitmap> transformedResource;

  private ReEncodingGifResourceEncoder encoder;
  private Options options;
  private File file;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    Application context = RuntimeEnvironment.application;

    ReEncodingGifResourceEncoder.Factory factory = mock(ReEncodingGifResourceEncoder.Factory.class);
    when(decoder.getNextFrame()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
    when(factory.buildDecoder(any(GifDecoder.BitmapProvider.class))).thenReturn(decoder);
    when(factory.buildParser()).thenReturn(parser);
    when(factory.buildEncoder()).thenReturn(gifEncoder);
    when(factory.buildFrameResource(anyBitmapOrNull(), any(BitmapPool.class)))
        .thenReturn(frameResource);

    // TODO Util.anyResource once Util is moved to testutil module (remove unchecked above!)
    when(frameTransformation.transform(anyContext(), any(Resource.class), anyInt(), anyInt()))
        .thenReturn(frameResource);

    when(gifDrawable.getFrameTransformation()).thenReturn(frameTransformation);
    when(gifDrawable.getBuffer()).thenReturn(ByteBuffer.allocate(0));

    when(resource.get()).thenReturn(gifDrawable);

    encoder = new ReEncodingGifResourceEncoder(context, mock(BitmapPool.class), factory);
    options = new Options();
    options.set(ReEncodingGifResourceEncoder.ENCODE_TRANSFORMATION, true);

    file = new File(context.getCacheDir(), "test");
  }

  @After
  public void tearDown() {
    // GC before delete() to release files on Windows (https://stackoverflow.com/a/4213208/253468)
    System.gc();
    if (file.exists() && !file.delete()) {
      throw new RuntimeException("Failed to delete file");
    }
  }

  @Test
  public void testEncodeStrategy_withEncodeTransformationTrue_returnsTransformed() {
    assertThat(encoder.getEncodeStrategy(options)).isEqualTo(EncodeStrategy.TRANSFORMED);
  }

  @Test
  public void testEncodeStrategy_withEncodeTransformationUnSet_returnsSource() {
    options.set(ReEncodingGifResourceEncoder.ENCODE_TRANSFORMATION, null);
    assertThat(encoder.getEncodeStrategy(options)).isEqualTo(EncodeStrategy.SOURCE);
  }

  @Test
  public void testEncodeStrategy_withEncodeTransformationFalse_returnsSource() {
    options.set(ReEncodingGifResourceEncoder.ENCODE_TRANSFORMATION, false);
    assertThat(encoder.getEncodeStrategy(options)).isEqualTo(EncodeStrategy.SOURCE);
  }

  @Test
  public void testEncode_withEncodeTransformationFalse_writesSourceDataToStream()
      throws IOException {
    options.set(ReEncodingGifResourceEncoder.ENCODE_TRANSFORMATION, false);
    String expected = "testString";
    byte[] data = expected.getBytes("UTF-8");
    when(gifDrawable.getBuffer()).thenReturn(ByteBuffer.wrap(data));

    assertTrue(encoder.encode(resource, file, options));
    assertThat(getEncodedData()).isEqualTo(expected);
  }

  @Test
  public void testEncode_WithEncodeTransformationFalse_whenOsThrows_returnsFalse()
      throws IOException {
    options.set(ReEncodingGifResourceEncoder.ENCODE_TRANSFORMATION, false);
    byte[] data = "testString".getBytes("UTF-8");
    when(gifDrawable.getBuffer()).thenReturn(ByteBuffer.wrap(data));

    assertThat(file.mkdirs()).isTrue();

    assertFalse(encoder.encode(resource, file, options));
  }

  @Test
  public void testReturnsFalseIfEncoderFailsToStart() {
    when(gifEncoder.start(any(OutputStream.class))).thenReturn(false);
    assertFalse(encoder.encode(resource, file, options));
  }

  @Test
  public void testSetsDataOnParserBeforeParsingHeader() {
    ByteBuffer data = ByteBuffer.allocate(1);
    when(gifDrawable.getBuffer()).thenReturn(data);

    GifHeader header = mock(GifHeader.class);
    when(parser.parseHeader()).thenReturn(header);

    encoder.encode(resource, file, options);

    InOrder order = inOrder(parser, decoder);
    order.verify(parser).setData(eq(data));
    order.verify(parser).parseHeader();
    order.verify(decoder).setData(header, data);
  }

  @Test
  public void testAdvancesDecoderBeforeAttemptingToGetFirstFrame() {
    when(gifEncoder.start(any(OutputStream.class))).thenReturn(true);
    when(decoder.getFrameCount()).thenReturn(1);
    when(decoder.getNextFrame()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));

    encoder.encode(resource, file, options);

    InOrder order = inOrder(decoder);
    order.verify(decoder).advance();
    order.verify(decoder).getNextFrame();
  }

  @Test
  public void testSetsDelayOnEncoderAfterAddingFrame() {
    when(gifEncoder.start(any(OutputStream.class))).thenReturn(true);
    when(gifEncoder.addFrame(anyBitmapOrNull())).thenReturn(true);

    when(decoder.getFrameCount()).thenReturn(1);
    when(decoder.getNextFrame()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565));
    int expectedIndex = 34;
    when(decoder.getCurrentFrameIndex()).thenReturn(expectedIndex);
    int expectedDelay = 5000;
    when(decoder.getDelay(eq(expectedIndex))).thenReturn(expectedDelay);

    encoder.encode(resource, file, options);

    InOrder order = inOrder(gifEncoder, decoder);
    order.verify(decoder).advance();
    order.verify(gifEncoder).addFrame(anyBitmapOrNull());
    order.verify(gifEncoder).setDelay(eq(expectedDelay));
    order.verify(decoder).advance();
  }

  @Test
  public void testWritesSingleFrameToEncoderAndReturnsTrueIfEncoderFinishes() {
    Bitmap frame = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    when(frameResource.get()).thenReturn(frame);

    when(decoder.getFrameCount()).thenReturn(1);
    when(decoder.getNextFrame()).thenReturn(frame);

    when(gifEncoder.start(any(OutputStream.class))).thenReturn(true);
    when(gifEncoder.addFrame(eq(frame))).thenReturn(true);
    when(gifEncoder.finish()).thenReturn(true);

    assertTrue(encoder.encode(resource, file, options));
    verify(gifEncoder).addFrame(eq(frame));
  }

  @Test
  public void testReturnsFalseIfAddingFrameFails() {
    when(decoder.getFrameCount()).thenReturn(1);
    when(decoder.getNextFrame()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));

    when(gifEncoder.start(any(OutputStream.class))).thenReturn(true);
    when(gifEncoder.addFrame(anyBitmapOrNull())).thenReturn(false);

    assertFalse(encoder.encode(resource, file, options));
  }

  @Test
  public void testReturnsFalseIfFinishingFails() {
    when(gifEncoder.start(any(OutputStream.class))).thenReturn(true);
    when(gifEncoder.finish()).thenReturn(false);

    assertFalse(encoder.encode(resource, file, options));
  }

  @Test
  public void testWritesTransformedBitmaps() {
    final Bitmap frame = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    when(decoder.getFrameCount()).thenReturn(1);
    when(decoder.getNextFrame()).thenReturn(frame);

    when(gifEncoder.start(any(OutputStream.class))).thenReturn(true);

    int expectedWidth = 123;
    int expectedHeight = 456;
    when(gifDrawable.getIntrinsicWidth()).thenReturn(expectedWidth);
    when(gifDrawable.getIntrinsicHeight()).thenReturn(expectedHeight);

    Bitmap transformedFrame = Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565);
    when(transformedResource.get()).thenReturn(transformedFrame);
    when(frameTransformation.transform(
            anyContext(), eq(frameResource), eq(expectedWidth), eq(expectedHeight)))
        .thenReturn(transformedResource);
    when(gifDrawable.getFrameTransformation()).thenReturn(frameTransformation);

    encoder.encode(resource, file, options);

    verify(gifEncoder).addFrame(eq(transformedFrame));
  }

  @Test
  public void testRecyclesFrameResourceBeforeWritingIfTransformedResourceIsDifferent() {
    when(decoder.getFrameCount()).thenReturn(1);
    when(frameTransformation.transform(anyContext(), eq(frameResource), anyInt(), anyInt()))
        .thenReturn(transformedResource);
    Bitmap expected = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
    when(transformedResource.get()).thenReturn(expected);

    when(gifEncoder.start(any(OutputStream.class))).thenReturn(true);

    encoder.encode(resource, file, options);

    InOrder order = inOrder(frameResource, gifEncoder);
    order.verify(frameResource).recycle();
    order.verify(gifEncoder).addFrame(eq(expected));
  }

  @Test
  public void testRecyclesTransformedResourceAfterWritingIfTransformedResourceIsDifferent() {
    when(decoder.getFrameCount()).thenReturn(1);
    Bitmap expected = Bitmap.createBitmap(100, 200, Bitmap.Config.RGB_565);
    when(transformedResource.get()).thenReturn(expected);
    when(frameTransformation.transform(anyContext(), eq(frameResource), anyInt(), anyInt()))
        .thenReturn(transformedResource);

    when(gifEncoder.start(any(OutputStream.class))).thenReturn(true);

    encoder.encode(resource, file, options);

    InOrder order = inOrder(transformedResource, gifEncoder);
    order.verify(gifEncoder).addFrame(eq(expected));
    order.verify(transformedResource).recycle();
  }

  @Test
  public void testRecyclesFrameResourceAfterWritingIfFrameResourceIsNotTransformed() {
    when(decoder.getFrameCount()).thenReturn(1);
    when(frameTransformation.transform(anyContext(), eq(frameResource), anyInt(), anyInt()))
        .thenReturn(frameResource);
    Bitmap expected = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888);
    when(frameResource.get()).thenReturn(expected);

    when(gifEncoder.start(any(OutputStream.class))).thenReturn(true);

    encoder.encode(resource, file, options);

    InOrder order = inOrder(frameResource, gifEncoder);
    order.verify(gifEncoder).addFrame(eq(expected));
    order.verify(frameResource).recycle();
  }

  @Test
  public void testWritesBytesDirectlyToDiskIfTransformationIsUnitTransformation() {
    when(gifDrawable.getFrameTransformation()).thenReturn(UnitTransformation.<Bitmap>get());
    String expected = "expected";
    when(gifDrawable.getBuffer()).thenReturn(ByteBuffer.wrap(expected.getBytes()));

    encoder.encode(resource, file, options);

    assertThat(getEncodedData()).isEqualTo(expected);

    verify(gifEncoder, never()).start(any(OutputStream.class));
    verify(parser, never()).setData(any(byte[].class));
    verify(parser, never()).parseHeader();
  }

  private String getEncodedData() {
    try {
      return new String(ByteBufferUtil.toBytes(ByteBufferUtil.fromFile(file)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Context anyContext() {
    return any(Context.class);
  }

  private static Bitmap anyBitmapOrNull() {
    return any();
  }
}
