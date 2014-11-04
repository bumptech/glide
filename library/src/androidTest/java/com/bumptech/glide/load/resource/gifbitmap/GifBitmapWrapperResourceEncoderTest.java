package com.bumptech.glide.load.resource.gifbitmap;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.resource.gif.GifDrawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GifBitmapWrapperResourceEncoderTest {
    private ResourceEncoder<Bitmap> bitmapEncoder;
    private ResourceEncoder<GifDrawable> gifEncoder;
    private GifBitmapWrapperResourceEncoder encoder;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        bitmapEncoder = mock(ResourceEncoder.class);
        gifEncoder = mock(ResourceEncoder.class);
        encoder = new GifBitmapWrapperResourceEncoder(bitmapEncoder, gifEncoder);
    }

    @Test
    public void testHasValidId() {
        String bitmapId = "bitmapId";
        when(bitmapEncoder.getId()).thenReturn(bitmapId);
        String gifId = "gifId";
        when(gifEncoder.getId()).thenReturn(gifId);

        String id = encoder.getId();
        assertThat(id).contains(bitmapId);
        assertThat(id).contains(gifId);
    }
}
