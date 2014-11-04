package com.bumptech.glide.load.resource.gif;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bumptech.glide.Priority;
import com.bumptech.glide.gifdecoder.GifDecoder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GifFrameModelLoaderTest {
    private GifFrameModelLoader loader;
    private GifDecoder decoder;

    @Before
    public void setUp() {
        loader = new GifFrameModelLoader();
        decoder = mock(GifDecoder.class);
    }

    @Test
    public void testFetcherIdIncludesFrameIndex() {
        String id = "asdfasd";
        int frameIndex = 124;
        when(decoder.getCurrentFrameIndex()).thenReturn(frameIndex);

        String fetcherId = loader.getResourceFetcher(decoder, 1, 2).getId();

        assertThat(fetcherId).contains(String.valueOf(frameIndex));
    }

    @Test
    public void testAlwaysReturnsGivenDecoderFromFetcher() throws Exception {
        assertEquals(decoder, loader.getResourceFetcher(decoder, 100, 100).loadData(Priority.NORMAL));
    }
}
