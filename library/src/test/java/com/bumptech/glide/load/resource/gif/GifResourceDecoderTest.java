package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.tests.GlideShadowLooper;
import com.bumptech.glide.tests.Util;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = GlideShadowLooper.class)
public class GifResourceDecoderTest {
    private GifResourceDecoder decoder;

    @Before
    public void setUp() {
        decoder = new GifResourceDecoder(Robolectric.application);
    }

    @Test
    public void testHasValidId() {
        Util.assertClassHasValidId(GifResourceDecoder.class, decoder.getId());
    }
}
