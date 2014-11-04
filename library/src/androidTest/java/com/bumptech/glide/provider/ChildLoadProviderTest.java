package com.bumptech.glide.provider;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;

@RunWith(JUnit4.class)
public class ChildLoadProviderTest {
    private ChildLoadHarness harness;

    @Before
    public void setUp() {
        harness = new ChildLoadHarness();
    }

    @Test
    public void testReturnsParentModelLoader() {
        when(harness.parent.getModelLoader()).thenReturn(harness.modelLoader);

        assertEquals(harness.modelLoader, harness.provider.getModelLoader());
    }

    @Test
    public void testReturnsParentSourceDecoderIfNoneIsSet() {
        when(harness.parent.getSourceDecoder()).thenReturn(harness.decoder);

        assertEquals(harness.decoder, harness.provider.getSourceDecoder());
    }

    @Test
    public void testReturnsChildSourceDecoderIfSet() {
        harness.provider.setSourceDecoder(harness.decoder);

        assertEquals(harness.decoder, harness.provider.getSourceDecoder());
    }

    @Test
    public void testReturnsParentCacheDecoderIfNoneIsSet() {
        when(harness.parent.getCacheDecoder()).thenReturn(harness.cacheDecoder);

        assertEquals(harness.cacheDecoder, harness.provider.getCacheDecoder());
    }

    @Test
    public void testReturnsChildCacheDecoderIfSet() {
        harness.provider.setCacheDecoder(harness.cacheDecoder);

        assertEquals(harness.cacheDecoder, harness.provider.getCacheDecoder());
    }

    @Test
    public void testReturnsParentEncoderIfNoneIsSet() {
        when(harness.parent.getEncoder()).thenReturn(harness.encoder);

        assertEquals(harness.encoder, harness.provider.getEncoder());
    }

    @Test
    public void testReturnsChildEncoderIfSet() {
        harness.provider.setEncoder(harness.encoder);

        assertEquals(harness.encoder, harness.provider.getEncoder());
    }

    @Test
    public void testReturnsParentTranscoderIfNoneIsSet() {
        when(harness.parent.getTranscoder()).thenReturn(harness.transcoder);

        assertEquals(harness.transcoder, harness.provider.getTranscoder());
    }

    @Test
    public void testReturnsChildTranscoderIfSet() {
        harness.provider.setTranscoder(harness.transcoder);

        assertEquals(harness.transcoder, harness.provider.getTranscoder());
    }

    @Test
    public void testReturnsParentSourceEncoderIfNoneIsSet() {
        when(harness.parent.getSourceEncoder()).thenReturn(harness.sourceEncoder);

        assertEquals(harness.sourceEncoder, harness.provider.getSourceEncoder());
    }

    @Test
    public void testReturnsChildSourceEncoderIfSet() {
        harness.provider.setSourceEncoder(harness.sourceEncoder);

        assertEquals(harness.sourceEncoder, harness.provider.getSourceEncoder());
    }

    @SuppressWarnings("unchecked")
    private static class ChildLoadHarness {
        ResourceEncoder<Object> encoder = mock(ResourceEncoder.class);
        ResourceDecoder<File, Object> cacheDecoder = mock(ResourceDecoder.class);
        ResourceDecoder<Object, Object> decoder = mock(ResourceDecoder.class);
        Encoder<Object> sourceEncoder = mock(Encoder.class);
        ModelLoader<Object, Object> modelLoader = mock(ModelLoader.class);
        LoadProvider<Object, Object, Object, Object> parent = mock(LoadProvider.class);
        ResourceTranscoder<Object, Object> transcoder = mock(ResourceTranscoder.class);
        ChildLoadProvider<Object, Object, Object, Object> provider =
                new ChildLoadProvider<Object, Object, Object, Object>(parent);

    }
}
