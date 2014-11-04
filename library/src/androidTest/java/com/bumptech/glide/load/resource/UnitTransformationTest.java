package com.bumptech.glide.load.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import com.bumptech.glide.load.engine.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class UnitTransformationTest {

    @Test
    public void testReturnsGivenResource() {
        Resource resource = mock(Resource.class);
        UnitTransformation transformation = UnitTransformation.get();
        assertEquals(resource, transformation.transform(resource, 10, 10));
    }

    @Test
    public void testHasEmptyId() {
        assertEquals("", UnitTransformation.get().getId());
    }
}