package com.bumptech.glide.provider;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.model.ModelLoader;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @SuppressWarnings("unchecked")
    private static class ChildLoadHarness {
        ResourceEncoder<Object> encoder = mock(ResourceEncoder.class);
        ResourceDecoder<InputStream, Object> cacheDecoder = mock(ResourceDecoder.class);
        ResourceDecoder<Object, Object> decoder = mock(ResourceDecoder.class);
        ModelLoader<Object, Object> modelLoader = mock(ModelLoader.class);
        LoadProvider<Object, Object, Object> parent = mock(LoadProvider.class);
        ChildLoadProvider<Object, Object, Object> provider = new ChildLoadProvider<Object, Object, Object>(parent);

    }
}
