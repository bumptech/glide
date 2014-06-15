package com.bumptech.glide.load.resource;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class NullCacheDecoderTest {
    private NullCacheDecoder decoder;

    @Before
    public void setUp() {
        decoder = new NullCacheDecoder();
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
