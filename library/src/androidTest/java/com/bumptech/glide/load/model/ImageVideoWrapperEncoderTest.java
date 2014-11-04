package com.bumptech.glide.load.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.ParcelFileDescriptor;

import com.bumptech.glide.load.Encoder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class ImageVideoWrapperEncoderTest {
    private Encoder<InputStream> streamEncoder;
    private Encoder<ParcelFileDescriptor> fileDescriptorEncoder;
    private ImageVideoWrapperEncoder encoder;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        streamEncoder = mock(Encoder.class);
        fileDescriptorEncoder = mock(Encoder.class);
        encoder = new ImageVideoWrapperEncoder(streamEncoder, fileDescriptorEncoder);
    }

    @Test
    public void testReturnsIdOfStreamAndFileDescriptorEncoders() {
        String streamId = "streamId";
        when(streamEncoder.getId()).thenReturn(streamId);
        String fileId = "fileId";
        when(fileDescriptorEncoder.getId()).thenReturn(fileId);

        String id = encoder.getId();

        assertThat(id).contains(streamId);
        assertThat(id).contains(fileId);
    }

    @Test
    public void testEncodesWithStreamEncoderIfInputStreamIsNotNull() {
        InputStream expected = new ByteArrayInputStream(new byte[2]);
        ImageVideoWrapper data = mock(ImageVideoWrapper.class);
        when(data.getStream()).thenReturn(expected);

        OutputStream os = new ByteArrayOutputStream();
        when(streamEncoder.encode(eq(expected), eq(os))).thenReturn(true);
        assertTrue(encoder.encode(data, os));

        verify(streamEncoder).encode(eq(expected), eq(os));
    }

    @Test
    public void testEncodesWithFileDescriptorEncoderIfFileDescriptorIsNotNullAndStreamIs() throws IOException {
        ParcelFileDescriptor expected = ParcelFileDescriptor.dup(FileDescriptor.err);
        ImageVideoWrapper data = mock(ImageVideoWrapper.class);
        when(data.getStream()).thenReturn(null);
        when(data.getFileDescriptor()).thenReturn(expected);

        OutputStream os = new ByteArrayOutputStream();
        when(fileDescriptorEncoder.encode(eq(expected), eq(os))).thenReturn(true);
        assertTrue(encoder.encode(data, os));

        verify(fileDescriptorEncoder).encode(eq(expected), eq(os));
    }
}