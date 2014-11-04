package com.bumptech.glide.load.model;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
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