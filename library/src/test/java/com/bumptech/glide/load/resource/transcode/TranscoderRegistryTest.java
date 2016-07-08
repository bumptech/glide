package com.bumptech.glide.load.resource.transcode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.io.File;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TranscoderRegistryTest {
  private TranscoderRegistry factories;

  @Before
  public void setUp() {
    factories = new TranscoderRegistry();
  }

  @Test
  public void testReturnsUnitDecoderIfClassesAreIdentical() {
    assertEquals(UnitTranscoder.get(), factories.get(Object.class, Object.class));
  }

  @Test
  public void testCanRegisterAndRetrieveResourceTranscoder() {
    @SuppressWarnings("unchecked")
    ResourceTranscoder<File, String> transcoder = mock(ResourceTranscoder.class);
    factories.register(File.class, String.class, transcoder);

    assertEquals(transcoder, factories.get(File.class, String.class));
  }

  @Test
  public void testDoesNotThrowIfRequestCanBeSatisfiedByUnitTranscoder() {
    // Assignable from.
    assertNotNull(factories.get(Integer.class, Number.class));
    // Equal to.
    assertNotNull(factories.get(Integer.class, Integer.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfNoTranscoderRegistered() {
    factories.get(File.class, Integer.class);
  }
}
