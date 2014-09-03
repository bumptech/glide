package com.bumptech.glide.load.resource.transcode;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.tests.Util;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        for (String fakeData : new String[] { "test", "1235asfklaw3", "@$@#"}) {
            byte[] expected = fakeData.getBytes();
            when(gifDrawable.getData()).thenReturn(expected);

            Resource<byte[]> transcoded = transcoder.transcode(resource);

            assertArrayEquals(expected, transcoded.get());
        }
    }

    @Test
    public void testReturnsValidId() {
        Util.assertClassHasValidId(GifDrawableBytesTranscoder.class, transcoder.getId());
    }
}
