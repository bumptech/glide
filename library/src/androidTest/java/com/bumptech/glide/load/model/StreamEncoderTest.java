package com.bumptech.glide.load.model;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class StreamEncoderTest {
    private StreamEncoder encoder;

    @Before
    public void setUp() {
        encoder = new StreamEncoder();
    }

    @Test
    public void testReturnsEmptyId() {
        assertEquals("", encoder.getId());
    }

    @Test
    public void testWritesDataFromInputStreamToOutputStream() {
        String fakeData = "SomeRandomFakeData";
        ByteArrayInputStream is = new ByteArrayInputStream(fakeData.getBytes());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        encoder.encode(is, os);

        assertEquals(fakeData, new String(os.toByteArray()));
    }
}