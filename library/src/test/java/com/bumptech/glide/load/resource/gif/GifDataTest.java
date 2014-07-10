package com.bumptech.glide.load.resource.gif;

import android.graphics.Bitmap;

import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.tests.GlideShadowLooper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = GlideShadowLooper.class)
public class GifDataTest {
    private GifData data;
    private byte[] bytes;
    private GifData.GifDrawableFactory factory;
    private GifHeader header;
    private String gifId;
    private int targetWidth;
    private int targetHeight;
    private int originalHeight;
    private int originalWidth;
    private BitmapPool bitmapPool;

    @Before
    public void setUp() {
        bitmapPool = mock(BitmapPool.class);
        gifId = "gifId";
        header = mock(GifHeader.class);
        bytes = new byte[] { 'G', 'I', 'F' };
        factory = mock(GifData.GifDrawableFactory.class);
        targetWidth = 123;
        targetHeight = 456;

        originalWidth = 100;
        originalHeight = 100;
        when(header.getWidth()).thenReturn(originalWidth);
        when(header.getHeight()).thenReturn(originalHeight);

        data = new GifData(Robolectric.application, bitmapPool, gifId, header, bytes, targetWidth, targetHeight,
                factory);
    }

    @Test
    public void testReturnsDecoderByteSize() {
        assertEquals(bytes.length, data.getByteSize());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReturnsSetTransformation() {
        Transformation<Bitmap> transformation = mock(Transformation.class);
        Resource<Bitmap> resource = mock(Resource.class);
        when(resource.get()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_4444));
        when(transformation.transform(any(Resource.class), anyInt(), anyInt())).thenReturn(resource);

        data.setFrameTransformation(transformation);
        assertEquals(transformation, data.getFrameTransformation());
    }

    @Test
    public void testBuildsDrawableWithFactory() {
        data.getDrawable();

        verify(factory).build(eq(Robolectric.application), any(GifDecoder.BitmapProvider.class),
                any(Transformation.class), eq(targetWidth), eq(targetHeight), eq(gifId), eq(header), eq(bytes),
                eq(originalWidth), eq(originalHeight));
    }

    @Test
    public void testReturnsDifferentDrawables() {
        data.getDrawable();
        data.getDrawable();

        verify(factory, times(2)).build(eq(Robolectric.application), any(GifDecoder.BitmapProvider.class),
                any(Transformation.class), eq(targetWidth), eq(targetHeight), eq(gifId), eq(header), eq(bytes),
                eq(originalWidth), eq(originalHeight));
    }

    @Test
    public void testCallsRecycleOnAllReturnedDrawablesWhenRecycled() {
        GifDrawable drawable = mock(GifDrawable.class);
        when(factory.build(eq(Robolectric.application), any(GifDecoder.BitmapProvider.class), any(Transformation.class),
                eq(targetWidth), eq(targetHeight), eq(gifId), eq(header), eq(bytes), eq(originalWidth),
                eq(originalHeight)))
                .thenReturn(drawable);

        int toGet = 10;
        for (int i = 0; i < toGet; i++) {
            data.getDrawable();
        }
        data.recycle();
        verify(drawable, times(toGet)).recycle();
    }

    @Test
    public void testPassesTransformedDimensionsToFactoryIfGivenTransformation() {
        int expectedWidth = 222;
        int expectedHeight = 444;

        Resource<Bitmap> resource = mock(Resource.class);
        when(resource.get()).thenReturn(Bitmap.createBitmap(expectedWidth, expectedHeight, Bitmap.Config.RGB_565));

        Transformation<Bitmap> transformation = mock(Transformation.class);
        when(transformation.transform(any(Resource.class), eq(targetWidth), eq(targetHeight)))
                .thenReturn(resource);

        data.setFrameTransformation(transformation);
        data.getDrawable();

        verify(factory).build(eq(Robolectric.application), any(GifDecoder.BitmapProvider.class), eq(transformation),
                eq(targetWidth), eq(targetHeight), eq(gifId), eq(header), eq(bytes), eq(expectedWidth),
                eq(expectedHeight));
    }
}
