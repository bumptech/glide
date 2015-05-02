package com.bumptech.glide.load.resource.transcode;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import com.bumptech.glide.load.engine.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class UnitTranscoderTest {

  @Test
  public void testReturnsTheGivenResource() {
    Resource resource = mock(Resource.class);
    ResourceTranscoder<Object, Object> unitTranscoder = UnitTranscoder.get();

    assertEquals(resource, unitTranscoder.transcode(resource));
  }
}
