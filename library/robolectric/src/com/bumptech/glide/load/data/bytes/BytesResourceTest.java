package com.bumptech.glide.load.data.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BytesResourceTest {

    @Test
    public void testReturnsGivenBytes() {
        byte[] bytes = new byte[0];
        BytesResource resource = new BytesResource(bytes);

        assertEquals(bytes, resource.get());
    }

    @Test
    public void testReturnsSizeOfGivenBytes() {
        byte[] bytes = new byte[123];
        BytesResource resource = new BytesResource(bytes);

        assertEquals(bytes.length, resource.getSize());
    }

}
