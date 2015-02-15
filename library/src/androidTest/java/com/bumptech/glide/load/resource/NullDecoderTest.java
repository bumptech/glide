package com.bumptech.glide.load.resource;

import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@RunWith(JUnit4.class)
public class NullDecoderTest {
  private NullDecoder<Object, Object> decoder;
  private Map<String, Object> options;

  @Before
  public void setUp() {
    decoder = new NullDecoder<>();
    options = Collections.emptyMap();
  }

  @Test
  public void testReturnsNull() throws IOException {
    assertNull(decoder.decode(new ByteArrayInputStream(new byte[0]), 100, 100, options));
  }
}
