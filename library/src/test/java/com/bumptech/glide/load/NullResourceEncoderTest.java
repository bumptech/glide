package com.bumptech.glide.load;

import com.bumptech.glide.load.engine.Resource;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.mockito.Mockito.mock;

public class NullResourceEncoderTest {

    @Test
    public void testEncode() throws Exception {
        NullResourceEncoder nullResourceEncoder = new NullResourceEncoder();
        Resource resource = mock(Resource.class);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        nullResourceEncoder.encode(resource, os);

        assertEquals(0, os.toByteArray().length);
    }

    @Test
    public void testReturnsFalseFromEncode() {
        NullResourceEncoder nullResourceEncoder = new NullResourceEncoder();

        assertFalse(nullResourceEncoder.encode(mock(Resource.class), new ByteArrayOutputStream()));
    }
}
