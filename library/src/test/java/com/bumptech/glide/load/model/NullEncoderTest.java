package com.bumptech.glide.load.model;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertFalse;

public class NullEncoderTest {
    private NullEncoder<Object> encoder;

    @Before
    public void setUp() {
        encoder = new NullEncoder<Object>();
    }

    @Test
    public void testReturnsFalse() {
        assertFalse(encoder.encode(new Object(), new ByteArrayOutputStream()));
    }

    @Test
    public void testReturnsEmptyId() {
        assertEquals("", encoder.getId());
    }
}