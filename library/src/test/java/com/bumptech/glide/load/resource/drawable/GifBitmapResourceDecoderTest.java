package com.bumptech.glide.load.resource.drawable;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GifBitmapResourceDecoderTest {
    private ResourceDecoder<ImageVideoWrapper, Bitmap> bitmapDecoder;
    private ResourceDecoder<InputStream, GifDrawable> gifDecoder;
    private GifBitmapResourceDecoder decoder;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        bitmapDecoder = mock(ResourceDecoder.class);
        gifDecoder = mock(ResourceDecoder.class);
        decoder = new GifBitmapResourceDecoder(Robolectric.application, bitmapDecoder, gifDecoder);
    }

    @Test
    public void testDecoderUsesGifDecoderResultIfGif() throws IOException {
        GifDrawable expected = mock(GifDrawable.class);
        Resource<GifDrawable> gifDrawableResource = mock(Resource.class);
        when(gifDrawableResource.get()).thenReturn(expected);
        when(gifDecoder.decode(any(InputStream.class), anyInt(), anyInt())).thenReturn(gifDrawableResource);

        byte[] data = new byte[] { 'G', 'I', 'F'};
        ImageVideoWrapper wrapper = new ImageVideoWrapper(new ByteArrayInputStream(data), null);

        Resource<GifBitmap> result = decoder.decode(wrapper, 100, 100);

        assertEquals(expected, result.get().getDrawable());
    }

    @Test
    public void testDecoderUsesBitmapDecoderIfStreamIsNotGif() throws IOException {
        Bitmap expected = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Resource<Bitmap> bitmapResource = mock(Resource.class);
        when(bitmapResource.get()).thenReturn(expected);
        when(bitmapDecoder.decode(any(ImageVideoWrapper.class), anyInt(), anyInt())).thenReturn(bitmapResource);

        byte[] data = new byte[] { 'A', 'I', 'F'};
        ImageVideoWrapper wrapper = new ImageVideoWrapper(new ByteArrayInputStream(data), null);

        Resource<GifBitmap> result = decoder.decode(wrapper, 100, 100);

        BitmapDrawable bitmapDrawable = (BitmapDrawable) result.get().getDrawable();

        assertEquals(expected, bitmapDrawable.getBitmap());
    }

    @Test
    public void testDecoderUsesBitmapDecoderIfIsFileDescriptor() throws IOException {
        Bitmap expected = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Resource<Bitmap> bitmapResource = mock(Resource.class);
        when(bitmapResource.get()).thenReturn(expected);
        when(bitmapDecoder.decode(any(ImageVideoWrapper.class), anyInt(), anyInt())).thenReturn(bitmapResource);

        ImageVideoWrapper wrapper = new ImageVideoWrapper(null, mock(ParcelFileDescriptor.class));

        Resource<GifBitmap> result = decoder.decode(wrapper, 100, 100);

        BitmapDrawable bitmapDrawable = (BitmapDrawable) result.get().getDrawable();

        assertEquals(expected, bitmapDrawable.getBitmap());
    }
}
