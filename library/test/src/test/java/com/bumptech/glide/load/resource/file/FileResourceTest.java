package com.bumptech.glide.load.resource.file;

import static org.junit.Assert.assertEquals;

import java.io.File;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
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
