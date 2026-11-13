package com.bumptech.glide.load.resource.bytes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BytesResourceTest {

  @Test
  public void returnsGivenBytes() {
    byte[] bytes = new byte[0];
    BytesResource resource = new BytesResource(bytes);

    assertEquals(bytes, resource.get());
  }

  @Test
  public void returnsSizeOfGivenBytes() {
    byte[] bytes = new byte[123];
    BytesResource resource = new BytesResource(bytes);

    assertEquals(bytes.length, resource.getSize());
  }

  @Test(expected = NullPointerException.class)
  public void throwsIfGivenNullBytes() {
    new BytesResource(null);
  }
}
