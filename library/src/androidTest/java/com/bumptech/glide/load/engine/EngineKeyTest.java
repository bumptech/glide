package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EngineKeyTest {
    private Harness harness;

    @Before
    public void setUp() {
        harness = new Harness();
    }

    private static class Harness {
        String id = "testId";
        int width = 1;
        int height = 2;
        ResourceDecoder cacheDecoder = mock(ResourceDecoder.class);
        ResourceDecoder decoder = mock(ResourceDecoder.class);
        Transformation transformation = mock(Transformation.class);
        ResourceEncoder encoder = mock(ResourceEncoder.class);
        ResourceTranscoder transcoder = mock(ResourceTranscoder.class);
        Encoder sourceEncoder = mock(Encoder.class);

        public Harness() {
            when(cacheDecoder.getId()).thenReturn("cacheDecoder");
            when(decoder.getId()).thenReturn("decoder");
            when(transformation.getId()).thenReturn("transformation");
            when(encoder.getId()).thenReturn("encoder");
            when(transcoder.getId()).thenReturn("transcoder");
            when(sourceEncoder.getId()).thenReturn("sourceEncoder");
        }

        public EngineKey build() {
            return new EngineKey(id, width, height, cacheDecoder, decoder, transformation, encoder, transcoder,
                    sourceEncoder);
        }
    }

    @Test
    public void testIsIdenticalWithSameArguments() {
        assertEquals(harness.build(), harness.build());
    }

    @Test
    public void testDiffersIfIdDiffers() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        EngineKey first = harness.build();
        harness.id = harness.id + "2";
        EngineKey second = harness.build();

        assertNotSame(first, second);
    }

    @Test
    public void testDiffersIfHeightDiffers() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        EngineKey first = harness.build();
        harness.height += 1;
        EngineKey second = harness.build();

        assertNotSame(first, second);
    }

    @Test
    public void testDiffersIfWidthDiffers() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        EngineKey first = harness.build();
        harness.width += 1;
        EngineKey second = harness.build();

        assertNotSame(first, second);
    }

    @Test
    public void testDiffersIfTransformationDiffers() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String id = "transformation";
        when(harness.transformation.getId()).thenReturn(id);
        EngineKey first = harness.build();
        harness.transformation = mock(Transformation.class);
        when(harness.transformation.getId()).thenReturn(id + "2");
        EngineKey second = harness.build();

        assertNotSame(first, second);
    }

    @Test
    public void testDiffersIfCacheDecoderDiffers() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String id = "cacheDecoder";
        when(harness.cacheDecoder.getId()).thenReturn(id);
        EngineKey first = harness.build();
        harness.cacheDecoder = mock(ResourceDecoder.class);
        when(harness.cacheDecoder.getId()).thenReturn(id + "2");
        EngineKey second = harness.build();

        assertNotSame(first, second);
    }

    @Test
    public void testDiffersIfDecoderDiffers() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String id = "decoder";
        when(harness.decoder.getId()).thenReturn(id);
        EngineKey first = harness.build();
        harness.decoder = mock(ResourceDecoder.class);
        when(harness.decoder.getId()).thenReturn(id + "2");
        EngineKey second = harness.build();

        assertNotSame(first, second);
    }

    @Test
    public void testDiffersIfEncoderDiffers() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String id = "encoder";
        when(harness.encoder.getId()).thenReturn(id);
        EngineKey first = harness.build();
        harness.encoder = mock(ResourceEncoder.class);
        when(harness.encoder.getId()).thenReturn(id + "2");
        EngineKey second = harness.build();

        assertNotSame(first, second);
    }

    @Test
    public void testDiffersWhenTranscoderDiffers() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String id = "transcoder";
        when(harness.transcoder.getId()).thenReturn(id);
        EngineKey first = harness.build();
        harness.transcoder = mock(ResourceTranscoder.class);
        when(harness.transcoder.getId()).thenReturn(id + "2");
        EngineKey second = harness.build();

        // The transcoder doesn't affect the cached data, so we don't expect the key digests to updated differently even
        // though the transcoder id isn't the same.
        assertNotSame(first, second, false);
    }

    @Test
    public void testDiffersWhenSourceEncoderDiffers() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String id = "sourceEncoder";
        when(harness.sourceEncoder.getId()).thenReturn(id);
        EngineKey first = harness.build();
        harness.sourceEncoder = mock(Encoder.class);
        when(harness.sourceEncoder.getId()).thenReturn(id + "2");
        EngineKey second = harness.build();

        assertNotSame(first, second);
    }

    private static void assertNotSame(EngineKey first, EngineKey second)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        assertNotSame(first, second, true);
    }

    private static void assertNotSame(EngineKey first, EngineKey second, boolean diskCacheDiffers)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        assertFalse(first.equals(second));
        assertTrue(first.hashCode() != second.hashCode());
        if (diskCacheDiffers) {
            MessageDigest firstDigest = MessageDigest.getInstance("SHA-1");
            first.updateDiskCacheKey(firstDigest);
            MessageDigest secondDigest = MessageDigest.getInstance("SHA-1");
            second.updateDiskCacheKey(secondDigest);

            assertFalse(Arrays.equals(firstDigest.digest(), secondDigest.digest()));
        }
    }
}