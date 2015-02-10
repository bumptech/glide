package com.bumptech.glide.load.resource.gif;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.gifencoder.AnimatedGifEncoder;
import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.UnitTransformation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class GifResourceEncoderTest {
    @Mock Resource<GifDrawable> resource;
    @Mock GifDecoder decoder;
    @Mock GifHeaderParser parser;
    @Mock AnimatedGifEncoder gifEncoder;
    @Mock Resource<Bitmap> frameResource;
    @Mock Transformation frameTransformation;
    @Mock GifDrawable gifDrawable;
    @Mock OutputStream os;

    private GifResourceEncoder encoder;
    private HashMap<String, Object> options;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        GifResourceEncoder.Factory factory = mock(GifResourceEncoder.Factory.class);
        when(factory.buildDecoder(any(GifDecoder.BitmapProvider.class))).thenReturn(decoder);
        when(factory.buildParser()).thenReturn(parser);
        when(factory.buildEncoder()).thenReturn(gifEncoder);
        when(factory.buildFrameResource(any(Bitmap.class), any(BitmapPool.class))).thenReturn(frameResource);

        when(frameTransformation.transform(any(Resource.class), anyInt(), anyInt())).thenReturn(frameResource);

        when(gifDrawable.getFrameTransformation()).thenReturn(frameTransformation);
        when(gifDrawable.getData()).thenReturn(new byte[0]);

        when(resource.get()).thenReturn(gifDrawable);

        encoder = new GifResourceEncoder(mock(BitmapPool.class), factory);
        options = new HashMap<>();
        options.put(GifResourceEncoder.KEY_ENCODE_TRANSFORMATION, true);
    }

    @Test
    public void testEncodeStrategy_withEncodeTransformationTrue_returnsTransformed() {
        assertEquals(EncodeStrategy.TRANSFORMED, encoder.getEncodeStrategy(options));
    }

    @Test
    public void testEncodeStrategy_withEncodeTransformationUnSet_returnsSource() {
        options.remove(GifResourceEncoder.KEY_ENCODE_TRANSFORMATION);
        assertEquals(EncodeStrategy.SOURCE, encoder.getEncodeStrategy(options));
    }

    @Test
    public void testEncodeStrategy_withEncodeTransformationFalse_returnsSource() {
        options.put(GifResourceEncoder.KEY_ENCODE_TRANSFORMATION, false);
        assertEquals(EncodeStrategy.SOURCE, encoder.getEncodeStrategy(options));
    }

    @Test
    public void testEncode_withEncodeTransformationFalse_writesSourceDataToStream() throws IOException {
        options.put(GifResourceEncoder.KEY_ENCODE_TRANSFORMATION, false);
        byte[] data = "testString".getBytes("UTF-8");
        when(gifDrawable.getData()).thenReturn(data);

        assertTrue(encoder.encode(resource, os, options));
        verify(os).write(eq(data));
    }

    @Test
    public void testEncode_WithEncodeTransformationFalse_whenOsThrows_returnsFalse() throws IOException {
        options.put(GifResourceEncoder.KEY_ENCODE_TRANSFORMATION, false);
        byte[] data = "testString".getBytes("UTF-8");
        when(gifDrawable.getData()).thenReturn(data);

        doThrow(new IOException()).when(os).write(eq(data));

        assertFalse(encoder.encode(resource, os, options));
    }

    @Test
    public void testReturnsFalseIfEncoderFailsToStart() {
        when(gifEncoder.start(eq(os))).thenReturn(false);
        assertFalse(encoder.encode(resource, os, options));
    }

    @Test
    public void testSetsDataOnParserBeforeParsingHeader() {
        byte[] data = new byte[1];
        when(gifDrawable.getData()).thenReturn(data);

        GifHeader header = mock(GifHeader.class);
        when(parser.parseHeader()).thenReturn(header);

        encoder.encode(resource, os, options);

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

        encoder.encode(resource, os, options);

        InOrder order = inOrder(decoder);
        order.verify(decoder).advance();
        order.verify(decoder).getNextFrame();
    }

    @Test
    public void testSetsDelayOnEncoderAfterAddingFrame() {
        when(gifEncoder.start(any(OutputStream.class))).thenReturn(true);
        when(gifEncoder.addFrame(any(Bitmap.class))).thenReturn(true);

        when(decoder.getFrameCount()).thenReturn(1);
        when(decoder.getNextFrame()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565));
        int expectedIndex = 34;
        when(decoder.getCurrentFrameIndex()).thenReturn(expectedIndex);
        int expectedDelay = 5000;
        when(decoder.getDelay(eq(expectedIndex))).thenReturn(expectedDelay);

        encoder.encode(resource, os, options);

        InOrder order = inOrder(gifEncoder, decoder);
        order.verify(decoder).advance();
        order.verify(gifEncoder).addFrame(any(Bitmap.class));
        order.verify(gifEncoder).setDelay(eq(expectedDelay));
        order.verify(decoder).advance();
    }

    @Test
    public void testWritesSingleFrameToEncoderAndReturnsTrueIfEncoderFinishes() {
        Bitmap frame = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        when(frameResource.get()).thenReturn(frame);

        when(decoder.getFrameCount()).thenReturn(1);
        when(decoder.getNextFrame()).thenReturn(frame);

        when(gifEncoder.start(eq(os))).thenReturn(true);
        when(gifEncoder.addFrame(eq(frame))).thenReturn(true);
        when(gifEncoder.finish()).thenReturn(true);

        assertTrue(encoder.encode(resource, os, options));
        verify(gifEncoder).addFrame(eq(frame));
    }

    @Test
    public void testReturnsFalseIfAddingFrameFails() {
        when(decoder.getFrameCount()).thenReturn(1);
        when(decoder.getNextFrame()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));

        when(gifEncoder.start(any(OutputStream.class))).thenReturn(true);
        when(gifEncoder.addFrame(any(Bitmap.class))).thenReturn(false);

        assertFalse(encoder.encode(resource, os, options));
    }

    @Test
    public void testReturnsFalseIfFinishingFails() {
        when(gifEncoder.start(any(OutputStream.class))).thenReturn(true);
        when(gifEncoder.finish()).thenReturn(false);

        assertFalse(encoder.encode(resource, os, options));
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
        final Resource<Bitmap> transformedResource = mock(Resource.class);
        when(transformedResource.get()).thenReturn(transformedFrame);
        Transformation<Bitmap> transformation = mock(Transformation.class);
        when(transformation.transform(eq(frameResource), eq(expectedWidth), eq(expectedHeight)))
                .thenReturn(transformedResource);
        when(gifDrawable.getFrameTransformation()).thenReturn(transformation);

        encoder.encode(resource, os, options);

        verify(gifEncoder).addFrame(eq(transformedFrame));
    }

    @Test
    public void testRecyclesFrameResourceBeforeWritingIfTransformedResourceIsDifferent() {
        when(decoder.getFrameCount()).thenReturn(1);
        Resource<Bitmap> transformedResource = mock(Resource.class);
        when(frameTransformation.transform(eq(frameResource), anyInt(), anyInt()))
                .thenReturn(transformedResource);
        Bitmap expected = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        when(transformedResource.get()).thenReturn(expected);

        when(gifEncoder.start(any(OutputStream.class))).thenReturn(true);

        encoder.encode(resource, os, options);

        InOrder order = inOrder(frameResource, gifEncoder);
        order.verify(frameResource).recycle();
        order.verify(gifEncoder).addFrame(eq(expected));
    }

    @Test
    public void testRecyclesTransformedResourceAfterWritingIfTransformedResourceIsDifferent() {
        when(decoder.getFrameCount()).thenReturn(1);
        Bitmap expected = Bitmap.createBitmap(100, 200, Bitmap.Config.RGB_565);
        Resource<Bitmap> transformedResource = mock(Resource.class);
        when(transformedResource.get()).thenReturn(expected);
        when(frameTransformation.transform(eq(frameResource), anyInt(), anyInt()))
                .thenReturn(transformedResource);

        when(gifEncoder.start(any(OutputStream.class))).thenReturn(true);

        encoder.encode(resource, os, options);

        InOrder order = inOrder(transformedResource, gifEncoder);
        order.verify(gifEncoder).addFrame(eq(expected));
        order.verify(transformedResource).recycle();
    }

    @Test
    public void testRecyclesFrameResourceAfterWritingIfFrameResourceIsNotTransformed() {
        when(decoder.getFrameCount()).thenReturn(1);
        when(frameTransformation.transform(eq(frameResource), anyInt(), anyInt()))
                .thenReturn(frameResource);
        Bitmap expected = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888);
        when(frameResource.get()).thenReturn(expected);

        when(gifEncoder.start(any(OutputStream.class))).thenReturn(true);

        encoder.encode(resource, os, options);

        InOrder order = inOrder(frameResource, gifEncoder);
        order.verify(gifEncoder).addFrame(eq(expected));
        order.verify(frameResource).recycle();
    }

    @Test
    public void testWritesBytesDirectlyToDiskIfTransformationIsUnitTransformation() throws IOException {
        when(gifDrawable.getFrameTransformation()).thenReturn(UnitTransformation.<Bitmap>get());
        byte[] expected = "expected".getBytes();
        when(gifDrawable.getData()).thenReturn(expected);

        encoder.encode(resource, os, options);

        verify(os).write(eq(expected));

        verify(gifEncoder, never()).start(any(OutputStream.class));
        verify(parser, never()).setData(any(byte[].class));
        verify(parser, never()).parseHeader();
    }
}
