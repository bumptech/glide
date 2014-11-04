package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class FileDescriptorBitmapDecoderTest {

    private FileDescriptorBitmapDecoder decoder;
    private BitmapPool bitmapPool;
    private VideoBitmapDecoder videoDecoder;
    private DecodeFormat decodeFormat;

    @Before
    public void setUp() {
        bitmapPool = mock(BitmapPool.class);
        videoDecoder = mock(VideoBitmapDecoder.class);
        decodeFormat = DecodeFormat.DEFAULT;
        decoder = new FileDescriptorBitmapDecoder(videoDecoder, bitmapPool, decodeFormat);
    }

    @Test
    public void testHasValidId() {
        assertEquals("FileDescriptorBitmapDecoder.com.bumptech.glide.load.data.bitmap", decoder.getId());
    }

    @Test
    public void testReturnsBitmapFromWrappedDecoderAsResource() throws IOException {
        ParcelFileDescriptor source = mock(ParcelFileDescriptor.class);
        int width = 100;
        int height = 200;
        Bitmap expected = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        when(videoDecoder.decode(eq(source), eq(bitmapPool), eq(width), eq(height), eq(decodeFormat)))
                .thenReturn(expected);

        Resource<Bitmap> bitmapResource = decoder.decode(source, width, height);

        assertEquals(expected, bitmapResource.get());
    }
}