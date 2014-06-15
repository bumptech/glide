package com.bumptech.glide.load.resource.transcode;

import com.bumptech.glide.Resource;
import com.bumptech.glide.load.resource.gif.GifData;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GifDataBytesTranscoderTest {
    private GifDataBytesTranscoder transcoder;
    private GifData gifData;
    private Resource<GifData> resource;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        gifData = mock(GifData.class);
        resource = mock(Resource.class);
        when(resource.get()).thenReturn(gifData);
        transcoder = new GifDataBytesTranscoder();
    }

    @Test
    public void testReturnsBytesOfGivenGifData() {
        for (String fakeData : new String[] { "test", "1235asfklaw3", "@$@#"}) {
            byte[] expected = fakeData.getBytes();
            when(gifData.getData()).thenReturn(expected);

            Resource<byte[]> transcoded = transcoder.transcode(resource);

            assertTrue(Arrays.equals(expected, transcoded.get()));
        }
    }

    @Test
    public void testReturnsValidId() {
        String expectedId = GifDataBytesTranscoder.class.getSimpleName() + "."
                + GifDataBytesTranscoder.class.getPackage().getName();
        assertEquals(expectedId, transcoder.getId());
    }
}
