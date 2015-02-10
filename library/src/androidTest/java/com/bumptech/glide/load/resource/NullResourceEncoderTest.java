package com.bumptech.glide.load.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import com.bumptech.glide.load.engine.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Map;

@RunWith(JUnit4.class)
public class NullResourceEncoderTest {

    private Map<String, Object> options;

    @Before
    public void setUp() {
        options = Collections.emptyMap();
    }

    @Test
    public void testEncode() throws Exception {
        NullResourceEncoder nullResourceEncoder = new NullResourceEncoder();
        Resource resource = mock(Resource.class);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        nullResourceEncoder.encode(resource, os, options);

        assertEquals(0, os.toByteArray().length);
    }

    @Test
    public void testReturnsFalseFromEncode() {
        NullResourceEncoder nullResourceEncoder = new NullResourceEncoder();

        assertFalse(nullResourceEncoder.encode(mock(Resource.class), new ByteArrayOutputStream(), options));
    }
}
