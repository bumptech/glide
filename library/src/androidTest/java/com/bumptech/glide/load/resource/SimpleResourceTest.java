package com.bumptech.glide.load.resource;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SimpleResourceTest {
    private Anything object;
    private SimpleResource resource;

    @Before
    public void setUp() {
        object = new Anything();
        resource = new SimpleResource(object);
    }

    @Test
    public void testReturnsGivenObject() {
        assertEquals(object, resource.get());
    }

    @Test
    public void testReturnsGivenObjectMultipleTimes() {
        assertEquals(object, resource.get());
        assertEquals(object, resource.get());
        assertEquals(object, resource.get());
    }

    private static class Anything { }
}
