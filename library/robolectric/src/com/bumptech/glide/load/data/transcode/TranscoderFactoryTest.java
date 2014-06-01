package com.bumptech.glide.load.data.transcode;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class TranscoderFactoryTest {
    private TranscoderFactory factories;

    @Before
    public void setUp() {
        factories = new TranscoderFactory();
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
