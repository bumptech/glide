package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.load.engine.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GifResourceEncoderTest {
    private Resource<GifDrawable> resource;
    private byte[] data;
    private GifResourceEncoder encoder;
    private GifDrawable gifDrawable;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        resource = mock(Resource.class);
        data = new byte[]{ 2, 3, 5, 8 };
        gifDrawable = mock(GifDrawable.class);
        when(gifDrawable.getData()).thenReturn(data);
        when(resource.get()).thenReturn(gifDrawable);
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
                super.write(buffer);
                throw new IOException("Test");
            }
        };

        assertFalse(encoder.encode(resource, os));
    }

    @Test
    public void testHasValidId() {
        assertEquals("", encoder.getId());
    }
}
