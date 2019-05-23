package com.bumptech.glide.load.resource.transcode;

import static com.bumptech.glide.tests.Util.mockResource;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GifDrawableBytesTranscoderTest {
  private GifDrawableBytesTranscoder transcoder;
  private GifDrawable gifDrawable;
  private Resource<GifDrawable> resource;

  @Before
  public void setUp() {
    gifDrawable = mock(GifDrawable.class);
    resource = mockResource();
    when(resource.get()).thenReturn(gifDrawable);
    transcoder = new GifDrawableBytesTranscoder();
  }

  @Test
  public void testReturnsBytesOfGivenGifDrawable() {
    for (String fakeData : new String[] {"test", "1235asfklaw3", "@$@#"}) {
      ByteBuffer expected = ByteBuffer.wrap(fakeData.getBytes(Charset.defaultCharset()));
      when(gifDrawable.getBuffer()).thenReturn(expected);

      Resource<byte[]> transcoded = transcoder.transcode(resource, new Options());

      assertArrayEquals(expected.array(), transcoded.get());
    }
  }
}
