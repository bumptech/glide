package com.bumptech.glide.load.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RunWith(JUnit4.class)
public class NullDecoderTest {
    private NullDecoder decoder;

    @Before
    public void setUp() {
        decoder = new NullDecoder();
    }

    //TODO: do we really want an empty id here?
    @Test
    public void testHasValidId() {
        assertEquals("", decoder.getId());
    }

    @Test
    public void testReturnsNull() throws IOException {
        assertNull(decoder.decode(new ByteArrayInputStream(new byte[0]), 100, 100));
    }
}
