package com.bumptech.glide.load.resource.transcode;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.gif.GifData;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.tests.Util;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GifDataDrawableTranscoderTest {
    private GifDataDrawableTranscoder transcoder;

    @Before
    public void setUp() {
        transcoder = new GifDataDrawableTranscoder();
    }

    @Test
    public void testReturnsDrawableFromResourceData() {
        GifDrawable expected = mock(GifDrawable.class);
        Resource<GifData> dataResource = mock(Resource.class);
        GifData gifData = mock(GifData.class);
        when(dataResource.get()).thenReturn(gifData);
        when(gifData.getDrawable()).thenReturn(expected);

        assertEquals(expected, transcoder.transcode(dataResource).get());
    }

    @Test
    public void testReturnsValidId() {
        Util.assertClassHasValidId(GifDataDrawableTranscoder.class, transcoder.getId());
    }
}
