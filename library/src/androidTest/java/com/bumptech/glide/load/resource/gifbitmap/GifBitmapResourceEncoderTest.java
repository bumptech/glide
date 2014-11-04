package com.bumptech.glide.load.resource.gifbitmap;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.gif.GifDrawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class GifBitmapResourceEncoderTest {
    private ResourceEncoder<Bitmap> bitmapEncoder;
    private ResourceEncoder<GifDrawable> gifEncoder;
    private GifBitmapWrapperResourceEncoder encoder;
    private Resource<GifBitmapWrapper> resource;
    private GifBitmapWrapper gifBitmap;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        bitmapEncoder = mock(ResourceEncoder.class);
        gifEncoder = mock(ResourceEncoder.class);
        encoder = new GifBitmapWrapperResourceEncoder(bitmapEncoder, gifEncoder);
        resource = mock(Resource.class);
        gifBitmap = mock(GifBitmapWrapper.class);
        when(resource.get()).thenReturn(gifBitmap);
    }

    @Test
    public void testEncodesWithBitmapEncoderIfHasBitmapResource() {
        Resource<Bitmap> bitmapResource = mock(Resource.class);
        when(gifBitmap.getBitmapResource()).thenReturn(bitmapResource);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        encoder.encode(resource, os);

        verify(bitmapEncoder).encode(eq(bitmapResource), eq(os));
    }

    @Test
    public void testReturnsBitmapEncoderResultIfHasBitmapResource() {
        Resource<Bitmap> bitmapResource = mock(Resource.class);
        when(gifBitmap.getBitmapResource()).thenReturn(bitmapResource);

        when(bitmapEncoder.encode(any(Resource.class), any(OutputStream.class))).thenReturn(true);
        assertTrue(encoder.encode(resource, new ByteArrayOutputStream()));

        when(bitmapEncoder.encode(any(Resource.class), any(OutputStream.class))).thenReturn(false);
        assertFalse(encoder.encode(resource, new ByteArrayOutputStream()));
    }

    @Test
    public void testEncodesWithGifEncoderIfHasGif() {
        Resource<GifDrawable> gifResource = mock(Resource.class);
        when(gifBitmap.getGifResource()).thenReturn(gifResource);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        encoder.encode(resource, os);

        verify(gifEncoder).encode(eq(gifResource), eq(os));
    }

    @Test
    public void testReturnsGifEncoderResultIfHasGifResource() {
        Resource<GifDrawable> gifResource = mock(Resource.class);
        when(gifBitmap.getGifResource()).thenReturn(gifResource);

        when(gifEncoder.encode(any(Resource.class), any(OutputStream.class))).thenReturn(true);
        assertTrue(encoder.encode(resource, new ByteArrayOutputStream()));

        when(gifEncoder.encode(any(Resource.class), any(OutputStream.class))).thenReturn(false);
        assertFalse(encoder.encode(resource, new ByteArrayOutputStream()));
    }

    @Test
    public void testReturnsValidId() {
        String gifId = "gifId";
        when(gifEncoder.getId()).thenReturn(gifId);
        String bitmapId = "bitmapId";
        when(bitmapEncoder.getId()).thenReturn(bitmapId);
        String id = encoder.getId();
        assertThat(id).contains(gifId);
        assertThat(id).contains(bitmapId);
    }
}
