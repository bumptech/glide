package com.bumptech.glide.load.resource.transcode;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;

@RunWith(JUnit4.class)
public class TranscoderRegistryTest {
    private TranscoderRegistry factories;

    @Before
    public void setUp() {
        factories = new TranscoderRegistry();
    }

    @Test
    public void testReturnsUnitDecoderIfClassesAreIdentical() {
        assertEquals(UnitTranscoder.get(), factories.get(Object.class, Object.class));
    }

    @Test
    public void testCanRegisterAndRetreiveResouceTranscoder() {
        ResourceTranscoder transcoder = mock(ResourceTranscoder.class);
        factories.register(File.class, Object.class, transcoder);

        assertEquals(transcoder, factories.get(File.class, Object.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfNoTranscoderRegistered() {
        factories.get(File.class, Object.class);
    }
}
