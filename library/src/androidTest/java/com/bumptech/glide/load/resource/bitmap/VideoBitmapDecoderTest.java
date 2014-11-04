package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.tests.Util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.FileDescriptor;
import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class VideoBitmapDecoderTest {
    private BitmapPool bitmapPool;
    private DecodeFormat decodeFormat;
    private ParcelFileDescriptor resource;
    private VideoBitmapDecoder decoder;
    private VideoBitmapDecoder.MediaMetadataRetrieverFactory factory;
    private MediaMetadataRetriever retriever;

    @Before
    public void setup() {
        bitmapPool = mock(BitmapPool.class);
        decodeFormat = DecodeFormat.ALWAYS_ARGB_8888;
        resource = mock(ParcelFileDescriptor.class);
        factory = mock(VideoBitmapDecoder.MediaMetadataRetrieverFactory.class);
        retriever = mock(MediaMetadataRetriever.class);
        when(factory.build()).thenReturn(retriever);
        decoder = new VideoBitmapDecoder(new VideoBitmapDecoder.MediaMetadataRetrieverFactory() {
            @Override
            public MediaMetadataRetriever build() {
                return factory.build();
            }
        });
    }

    @Test
    public void testReturnsRetrievedFrameForResource() throws IOException {
        Bitmap expected = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        when(retriever.getFrameAtTime()).thenReturn(expected);

        FileDescriptor toSet = FileDescriptor.in;
        when(resource.getFileDescriptor()).thenReturn(toSet);
        Bitmap result = decoder.decode(resource, bitmapPool, 100, 100, decodeFormat);

        verify(retriever).setDataSource(eq(toSet));
        assertEquals(expected, result);
    }

    @Test
    public void testReleasesMediaMetadataRetriever() throws IOException {
        decoder.decode(resource, bitmapPool, 1, 2, decodeFormat);

        verify(retriever).release();
    }

    @Test
    public void testClosesResource() throws IOException {
        decoder.decode(resource, bitmapPool, 1, 2, decodeFormat);

        verify(resource).close();
    }

    @Test
    public void testHasValidId() {
        Util.assertClassHasValidId(VideoBitmapDecoder.class, decoder.getId());
    }
}
