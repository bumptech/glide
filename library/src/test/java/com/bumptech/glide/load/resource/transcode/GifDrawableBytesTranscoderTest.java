package com.bumptech.glide.load.resource.transcode;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.gif.GifDrawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.ByteBuffer;

@RunWith(JUnit4.class)
public class GifDrawableBytesTranscoderTest {
  private GifDrawableBytesTranscoder transcoder;
  private GifDrawable gifDrawable;
  private Resource<GifDrawable> resource;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    gifDrawable = mock(GifDrawable.class);
    resource = mock(Resource.class);
    when(resource.get()).thenReturn(gifDrawable);
    transcoder = new GifDrawableBytesTranscoder();
  }

  @Test
  public void testReturnsBytesOfGivenGifDrawable() {
    for (String fakeData : new String[] { "test", "1235asfklaw3", "@$@#" }) {
      ByteBuffer expected = ByteBuffer.wrap(fakeData.getBytes());
      when(gifDrawable.getBuffer()).thenReturn(expected);

      Resource<byte[]> transcoded = transcoder.transcode(resource);

      assertArrayEquals(expected.array(), transcoded.get());
    }
  }
}
