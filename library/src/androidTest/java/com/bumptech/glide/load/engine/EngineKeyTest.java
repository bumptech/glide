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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests if {@link EngineKey} {@link Object#hashCode() hashCode} and {@link Object#equals(Object) equals}
 * and SHA-1 disk cache key are different on any difference in ID or existence of a certain workflow part.
 * Also checking whether the equals method is symmetric.
 *
 * @see #assertDifferent
 */
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
    public void testDiffersIfIdDiffers() throws Exception {
        EngineKey first = harness.build();
        harness.id = harness.id + "2";
        EngineKey second = harness.build();

        assertDifferent(first, second);
    }

    @Test
    public void testDiffersIfHeightDiffers() throws Exception {
        EngineKey first = harness.build();
        harness.height += 1;
        EngineKey second = harness.build();

        assertDifferent(first, second);
    }

    @Test
    public void testDiffersIfWidthDiffers() throws Exception {
        EngineKey first = harness.build();
        harness.width += 1;
        EngineKey second = harness.build();

        assertDifferent(first, second);
    }

    @Test
    public void testDiffersIfTransformationDiffers() throws Exception {
        String id = "transformation";
        when(harness.transformation.getId()).thenReturn(id);
        EngineKey first = harness.build();
        harness.transformation = mock(Transformation.class);
        when(harness.transformation.getId()).thenReturn(id + "2");
        EngineKey second = harness.build();

        assertDifferent(first, second);
    }

    @Test
    public void testDiffersIfTransformationMissing() throws Exception {
        EngineKey first = harness.build();
        harness.transformation = null;
        EngineKey second = harness.build();

        assertDifferent(first, second);
    }

    @Test
    public void testDiffersIfCacheDecoderDiffers() throws Exception {
        String id = "cacheDecoder";
        when(harness.cacheDecoder.getId()).thenReturn(id);
        EngineKey first = harness.build();
        harness.cacheDecoder = mock(ResourceDecoder.class);
        when(harness.cacheDecoder.getId()).thenReturn(id + "2");
        EngineKey second = harness.build();

        assertDifferent(first, second);
    }

    @Test
    public void testDiffersIfCacheDecoderMissing() throws Exception {
        EngineKey first = harness.build();
        harness.cacheDecoder = null;
        EngineKey second = harness.build();

        assertDifferent(first, second);
    }

    @Test
    public void testDiffersIfDecoderDiffers() throws Exception {
        String id = "decoder";
        when(harness.decoder.getId()).thenReturn(id);
        EngineKey first = harness.build();
        harness.decoder = mock(ResourceDecoder.class);
        when(harness.decoder.getId()).thenReturn(id + "2");
        EngineKey second = harness.build();

        assertDifferent(first, second);
    }

    @Test
    public void testDiffersIfDecoderMissing() throws Exception {
        EngineKey first = harness.build();
        harness.decoder = null;
        EngineKey second = harness.build();

        assertDifferent(first, second);
    }

    @Test
    public void testDiffersIfEncoderDiffers() throws Exception {
        String id = "encoder";
        when(harness.encoder.getId()).thenReturn(id);
        EngineKey first = harness.build();
        harness.encoder = mock(ResourceEncoder.class);
        when(harness.encoder.getId()).thenReturn(id + "2");
        EngineKey second = harness.build();

        assertDifferent(first, second);
    }

    @Test
    public void testDiffersIfEncoderMissing() throws Exception {
        EngineKey first = harness.build();
        harness.encoder = null;
        EngineKey second = harness.build();

        assertDifferent(first, second);
    }

    @Test
    public void testDiffersWhenTranscoderDiffers() throws Exception {
        String id = "transcoder";
        when(harness.transcoder.getId()).thenReturn(id);
        EngineKey first = harness.build();
        harness.transcoder = mock(ResourceTranscoder.class);
        when(harness.transcoder.getId()).thenReturn(id + "2");
        EngineKey second = harness.build();

        // The transcoder doesn't affect the cached data,
        // so we don't expect the key digests to updated differently even though the transcoder id isn't the same.
        assertDifferent(first, second, false);
        assertDifferent(second, first, false);
    }

    @Test
    public void testDiffersIfTranscoderMissing() throws Exception {
        EngineKey first = harness.build();
        harness.transcoder = null;
        EngineKey second = harness.build();

        assertDifferent(first, second, false);
        assertDifferent(second, first, false);
    }

    @Test
    public void testDiffersWhenSourceEncoderDiffers() throws Exception {
        String id = "sourceEncoder";
        when(harness.sourceEncoder.getId()).thenReturn(id);
        EngineKey first = harness.build();
        harness.sourceEncoder = mock(Encoder.class);
        when(harness.sourceEncoder.getId()).thenReturn(id + "2");
        EngineKey second = harness.build();

        assertDifferent(first, second);
    }

    @Test
    public void testDiffersIfSourceEncoderMissing() throws Exception {
        EngineKey first = harness.build();
        harness.sourceEncoder = null;
        EngineKey second = harness.build();

        assertDifferent(first, second);
    }

    private static void assertDifferent(EngineKey first, EngineKey second)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        assertDifferent(first, second, true);
        assertDifferent(second, first, true);
    }

    private static void assertDifferent(EngineKey first, EngineKey second, boolean diskCacheDiffers)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        assertNotEquals(first, second);
        assertNotEquals(first.hashCode(), second.hashCode());

        if (diskCacheDiffers) {
            MessageDigest firstDigest = MessageDigest.getInstance("SHA-1");
            first.updateDiskCacheKey(firstDigest);
            MessageDigest secondDigest = MessageDigest.getInstance("SHA-1");
            second.updateDiskCacheKey(secondDigest);

            assertThat(firstDigest.digest(), not(equalTo(secondDigest.digest())));
        }
    }
}
