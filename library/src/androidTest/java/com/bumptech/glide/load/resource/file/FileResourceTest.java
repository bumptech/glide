package com.bumptech.glide.load.resource.file;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertEquals;

public class FileResourceTest {

    private File file;
    private FileResource resource;

    @Before
    public void setUp() {
        file = new File("Test");
        resource = new FileResource(file);
    }

    @Test
    public void testReturnsGivenFile() {
        assertEquals(file, resource.get());
    }
}