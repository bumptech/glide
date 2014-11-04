package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.tests.Util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class ImageVideoBitmapDecoderTest {
    private ImageVideoDecoderHarness harness;

    @Before
    public void setUp() {
        harness = new ImageVideoDecoderHarness();
    }

    @Test
    public void testDecodesWithStreamDecoderFirst() throws IOException {
        when(harness.streamDecoder.decode(any(InputStream.class), anyInt(), anyInt())).thenReturn(harness.result);
        when(harness.wrapper.getStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        Resource<Bitmap> decoded = harness.decoder.decode(harness.wrapper, 100, 100);

        assertEquals(harness.result, decoded);
    }

    @Test
    public void testDecodesWithFileDecoderIfStreamFails() throws IOException {
        when(harness.streamDecoder.decode(any(InputStream.class), anyInt(), anyInt())).thenReturn(null);
        when(harness.wrapper.getFileDescriptor()).thenReturn(mock(ParcelFileDescriptor.class));
        when(harness.fileDescriptorDecoder.decode(any(ParcelFileDescriptor.class), anyInt(), anyInt()))
                .thenReturn(harness.result);

        Resource<Bitmap> decoded = harness.decoder.decode(harness.wrapper, 100, 100);

        assertEquals(harness.result, decoded);
    }
    @Test
    public void testDecodesWithFileDecoderIfStreamThrows() throws IOException {
        when(harness.streamDecoder.decode(any(InputStream.class), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("test"));
        when(harness.wrapper.getFileDescriptor()).thenReturn(mock(ParcelFileDescriptor.class));
        when(harness.fileDescriptorDecoder.decode(any(ParcelFileDescriptor.class), anyInt(), anyInt()))
                .thenReturn(harness.result);

        Resource<Bitmap> decoded = harness.decoder.decode(harness.wrapper, 100, 100);

        assertEquals(harness.result, decoded);
    }

    @Test
    public void testReturnsValidId() {
        Util.assertClassHasValidId(ImageVideoBitmapDecoder.class, harness.decoder.getId());
    }

    @Test
    public void testDoesNotTryToDecodeNullStream() throws IOException {
        when(harness.wrapper.getStream()).thenReturn(null);
        when(harness.wrapper.getFileDescriptor()).thenReturn(mock(ParcelFileDescriptor.class));
        when(harness.fileDescriptorDecoder.decode(any(ParcelFileDescriptor.class), anyInt(), anyInt()))
                .thenReturn(harness.result);

        assertEquals(harness.result, harness.decoder.decode(harness.wrapper, 100, 100));
        verify(harness.streamDecoder, never()).decode(any(InputStream.class), anyInt(), anyInt());
    }

    @Test
    public void testDoesNotTryToDecodeNullFileDescriptor() throws IOException {
        when(harness.wrapper.getStream()).thenReturn(null);
        when(harness.wrapper.getFileDescriptor()).thenReturn(null);

        harness.decoder.decode(harness.wrapper, 100, 102);

        verify(harness.fileDescriptorDecoder, never()).decode(any(ParcelFileDescriptor.class), anyInt(), anyInt());
    }

    @Test
    public void testHasValidId() {
        Util.assertClassHasValidId(ImageVideoBitmapDecoder.class, harness.decoder.getId());
    }

    @SuppressWarnings("unchecked")
    private static class ImageVideoDecoderHarness {
        Resource<Bitmap> result = mock(Resource.class);
        ResourceDecoder<InputStream, Bitmap> streamDecoder = mock(ResourceDecoder.class);
        ResourceDecoder<ParcelFileDescriptor, Bitmap> fileDescriptorDecoder = mock(ResourceDecoder.class);
        ImageVideoBitmapDecoder decoder = new ImageVideoBitmapDecoder(streamDecoder, fileDescriptorDecoder);
        ImageVideoWrapper wrapper = mock(ImageVideoWrapper.class);
    }
}
