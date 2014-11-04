package com.bumptech.glide.load.resource.file;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@RunWith(JUnit4.class)
public class FileToStreamDecoderTest {

    private ResourceDecoder<InputStream, Object> streamDecoder;
    private FileToStreamDecoder<Object> decoder;
    private FileToStreamDecoder.FileOpener fileOpener;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        fileOpener = mock(FileToStreamDecoder.FileOpener.class);
        streamDecoder = mock(ResourceDecoder.class);
        decoder = new FileToStreamDecoder<Object>(streamDecoder, fileOpener);
    }

    @Test
    public void testHasEmptyId() {
        assertEquals("", decoder.getId());
    }

    @Test
    public void testReturnsResourceFromStreamDecoder() throws IOException {
        File file = new File("test");
        InputStream expected = new ByteArrayInputStream(new byte[0]);
        when(fileOpener.open(eq(file))).thenReturn(expected);
        Resource<Object> resource = mock(Resource.class);
        int width = 123;
        int height = 456;

        when(streamDecoder.decode(eq(expected), eq(width), eq(height))).thenReturn(resource);

        assertEquals(resource, decoder.decode(file, width, height));
    }

    @Test
    public void testClosesStream() throws IOException {
        InputStream is = mock(InputStream.class);
        when(fileOpener.open(any(File.class))).thenReturn(is);

        decoder.decode(new File("test"), 100, 100);

        verify(is).close();
    }

    @Test
    public void testClosesStreamIfStreamDecoderThrows() throws IOException {
        InputStream is = mock(InputStream.class);
        when(fileOpener.open(any(File.class))).thenReturn(is);

        when(streamDecoder.decode(eq(is), anyInt(), anyInt())).thenThrow(new IOException("Test streamDecoder failed"));

        try {
            decoder.decode(new File("test"), 100, 100);
        } catch (IOException e) {
            // Expected.
        }

        verify(is).close();
    }
}