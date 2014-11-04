package com.bumptech.glide.load.resource.gifbitmap;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.resource.bitmap.ImageHeaderParser;
import com.bumptech.glide.load.resource.gif.GifDrawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class GifBitmapWrapperResourceDecoderTest {
    private ResourceDecoder<ImageVideoWrapper, Bitmap> bitmapDecoder;
    private ResourceDecoder<InputStream, GifDrawable> gifDecoder;
    private GifBitmapWrapperResourceDecoder decoder;
    private GifBitmapWrapperResourceDecoder.ImageTypeParser parser;
    private ImageVideoWrapper source;
    private GifBitmapWrapperResourceDecoder.BufferedStreamFactory streamFactory;
    private InputStream bis;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        bitmapDecoder = mock(ResourceDecoder.class);
        gifDecoder = mock(ResourceDecoder.class);
        parser = mock(GifBitmapWrapperResourceDecoder.ImageTypeParser.class);
        streamFactory = mock(GifBitmapWrapperResourceDecoder.BufferedStreamFactory.class);
        decoder = new GifBitmapWrapperResourceDecoder(bitmapDecoder, gifDecoder, mock(BitmapPool.class), parser,
                streamFactory);

        source = mock(ImageVideoWrapper.class);
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(source.getStream()).thenReturn(is);
        bis = mock(InputStream.class);
        when(streamFactory.build(eq(is), any(byte[].class))).thenReturn(bis);
    }

    @Test
    public void testDecodesGifTypeWithGifDecoder() throws IOException {
        when(parser.parse(eq(bis))).thenReturn(ImageHeaderParser.ImageType.GIF);
        int width = 100;
        int height = 200;
        Resource<GifDrawable> expected = mockGifResource();

        when(gifDecoder.decode(any(InputStream.class), eq(width), eq(height))).thenReturn(expected);

        Resource<GifBitmapWrapper> result = decoder.decode(source, width, height);

        assertEquals(expected, result.get().getGifResource());
    }

    @Test
    public void testDecodesBitmapTypeWithBitmapDecoder() throws IOException {
        when(parser.parse(eq(bis))).thenReturn(ImageHeaderParser.ImageType.JPEG);
        int width = 150;
        int height = 101;
        Resource<Bitmap> expected = mock(Resource.class);

        when(bitmapDecoder.decode(any(ImageVideoWrapper.class), eq(width), eq(height))).thenReturn(expected);

        Resource<GifBitmapWrapper> result = decoder.decode(source, width, height);

        assertEquals(expected, result.get().getBitmapResource());
    }

    @Test
    public void testReturnsGifResourceIfBothGifAndBitmapDecodersCanDecode() throws IOException {
        when(parser.parse(eq(bis))).thenReturn(ImageHeaderParser.ImageType.GIF);
        int width = 101;
        int height = 102;
        Resource<GifDrawable> expected = mockGifResource();
        when(gifDecoder.decode(any(InputStream.class), eq(width), eq(height))).thenReturn(expected);
        when(bitmapDecoder.decode(any(ImageVideoWrapper.class), eq(width), eq(height)))
                .thenReturn(mock(Resource.class));

        Resource<GifBitmapWrapper> result = decoder.decode(source, width, height);

        assertEquals(expected, result.get().getGifResource());
    }

    @Test
    public void testBitmapDecoderIsGivenImageVideoWrapperWithBufferedStreamIfStreamIsNotNull() throws IOException {
        when(parser.parse(eq(bis))).thenReturn(ImageHeaderParser.ImageType.PNG);

        when(bitmapDecoder.decode(any(ImageVideoWrapper.class), anyInt(), anyInt())).thenAnswer(
                new Answer<Resource<Bitmap>>() {
                    @Override
                    public Resource<Bitmap> answer(InvocationOnMock invocation) throws Throwable {
                        ImageVideoWrapper wrapper = (ImageVideoWrapper) invocation.getArguments()[0];
                        assertEquals(bis, wrapper.getStream());
                        return mock(Resource.class);
                    }
                });

        decoder.decode(source, 100, 100);

        verify(bitmapDecoder).decode(any(ImageVideoWrapper.class), anyInt(), anyInt());
    }

    @Test
    public void testDecodesBitmapTypeWhenGifTypeButGifDecoderFails() throws IOException {
        when(parser.parse(eq(bis))).thenReturn(ImageHeaderParser.ImageType.GIF);
        when(gifDecoder.decode(any(InputStream.class), anyInt(), anyInt())).thenReturn(null);

        Resource<Bitmap> expected = mock(Resource.class);
        when(bitmapDecoder.decode(any(ImageVideoWrapper.class), anyInt(), anyInt())).thenReturn(expected);

        Resource<GifBitmapWrapper> result = decoder.decode(source, 100, 100);

        assertEquals(expected, result.get().getBitmapResource());
    }

    @Test
    public void testReturnsBitmapWhenGifTypeButGifHasSingleFrame() throws IOException {
        Bitmap firstFrame = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Resource<GifDrawable> gifResource = mockGifResource();
        when(gifResource.get().getFrameCount()).thenReturn(1);
        when(gifResource.get().getFirstFrame()).thenReturn(firstFrame);

        when(parser.parse(eq(bis))).thenReturn(ImageHeaderParser.ImageType.GIF);
        when(gifDecoder.decode(any(InputStream.class), anyInt(), anyInt())).thenReturn(gifResource);

        Resource<GifBitmapWrapper> result = decoder.decode(source, 100, 100);

        assertEquals(firstFrame, result.get().getBitmapResource().get());
    }

    @Test
    public void testDoesNotCallBitmapDecoderWhenGifTypeButGifHasSingleFrame() throws IOException {
        Bitmap firstFrame = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Resource<GifDrawable> gifResource = mockGifResource();
        when(gifResource.get().getFrameCount()).thenReturn(1);
        when(gifResource.get().getFirstFrame()).thenReturn(firstFrame);

        when(parser.parse(eq(bis))).thenReturn(ImageHeaderParser.ImageType.GIF);
        when(gifDecoder.decode(any(InputStream.class), anyInt(), anyInt())).thenReturn(gifResource);

        decoder.decode(source, 100, 100);

        verify(bitmapDecoder, never()).decode(any(ImageVideoWrapper.class), anyInt(), anyInt());
    }

    @Test
    public void testDoesNotRecycleGifResourceWhenGifTypeButGifHasSingleFrame() throws IOException {
        Resource<GifDrawable> gifResource = mockGifResource();
        when(gifResource.get().getFrameCount()).thenReturn(1);

        when(parser.parse(eq(bis))).thenReturn(ImageHeaderParser.ImageType.GIF);
        when(gifDecoder.decode(any(InputStream.class), anyInt(), anyInt())).thenReturn(gifResource);
        when(gifResource.get().getFirstFrame()).thenReturn(Bitmap.createBitmap(50, 50, Bitmap.Config.RGB_565));

        decoder.decode(source, 100, 100);

        verify(gifResource, never()).recycle();
    }

    @Test
    public void testDoesNotTryToParseTypeOrDecodeNullStream() throws IOException {
        when(source.getFileDescriptor()).thenReturn(mock(ParcelFileDescriptor.class));
        when(source.getStream()).thenReturn(null);

        Resource<Bitmap> expected = mock(Resource.class);
        when(bitmapDecoder.decode(eq(source), anyInt(), anyInt())).thenReturn(expected);

        Resource<GifBitmapWrapper> result = decoder.decode(source, 100, 100);
        assertEquals(expected, result.get().getBitmapResource());
    }

    @Test
    public void testReturnsNullResourceIfBothBitmapDecoderAndGifDecoderFail() throws IOException {
        Resource<GifBitmapWrapper> result = decoder.decode(source, 100, 100);
        assertNull(result);
    }

    @Test
    public void testMarksAndResetsInputStreamBeforeAndAfterParsingType() throws IOException {
        decoder.decode(source, 100, 100);

        InOrder order = inOrder(bis, parser);
        order.verify(bis).mark(eq(GifBitmapWrapperResourceDecoder.MARK_LIMIT_BYTES));
        order.verify(parser).parse(eq(bis));
        order.verify(bis).reset();
    }

    @Test
    public void testHasValidId() {
        String bitmapId = "bitmapId";
        when(bitmapDecoder.getId()).thenReturn(bitmapId);
        String gifId = "gifId";
        when(gifDecoder.getId()).thenReturn(gifId);

        String id = decoder.getId();
        assertThat(id).contains(bitmapId);
        assertThat(id).contains(gifId);
    }

    @SuppressWarnings("unchecked")
    private static Resource<GifDrawable> mockGifResource() {
        GifDrawable drawable = mock(GifDrawable.class);
        // Something > 1.
        when(drawable.getFrameCount()).thenReturn(4);
        Resource<GifDrawable> resource = mock(Resource.class);
        when(resource.get()).thenReturn(drawable);
        return resource;
    }
}
