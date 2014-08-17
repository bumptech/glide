package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.gifdecoder.GifDecoder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class GifFrameManagerTest {

    @Test(expected = NullPointerException.class)
    public void testThrowsIfTransformationIsNull() {
        new GifFrameManager(Robolectric.application, mock(GifDecoder.class), null, 100, 100, 75, 75);
    }
}