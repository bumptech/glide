package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GifResourceEncoderTest {
    private Resource<GifDrawable> resource;
    private byte[] data;
    private GifResourceEncoder encoder;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        resource = mock(Resource.class);
        data = new byte[]{ 2, 3, 5, 8 };
        GifDecoder decoder = mock(GifDecoder.class);
        GifDrawable drawable = mock(GifDrawable.class);
        when(drawable.getDecoder()).thenReturn(decoder);
        when(resource.get()).thenReturn(drawable);
        when(decoder.getData()).thenReturn(data);
        encoder = new GifResourceEncoder();
    }

    @Test
    public void testWritesDataToOutputStream() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        encoder.encode(resource, os);

        assertTrue(Arrays.equals(data, os.toByteArray()));
    }

    @Test
    public void testReturnsTrueIfWriteCompletes() {
        assertTrue(encoder.encode(resource, new ByteArrayOutputStream()));
    }

    @Test
    public void testReturnsFalseIfWriteFails() {
        OutputStream os = new ByteArrayOutputStream() {
            @Override
            public void write(byte[] buffer) throws IOException {
                super.write(buffer);    //To change body of overridden methods use File | Settings | File Templates.
                throw new IOException("Test");
            }
        };

        assertFalse(encoder.encode(resource, os));
    }
}
