package com.bumptech.glide.load.resource.gifbitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.gif.GifData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GifBitmapTransformationTest {
    private Transformation<Bitmap> bitmapTransformation;
    private GifBitmapTransformation transformation;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        bitmapTransformation = mock(Transformation.class);
        transformation = new GifBitmapTransformation(Robolectric.application, bitmapTransformation);
    }

    @Test
    public void testReturnsWrappedTransformationId() {
        String expectedId = "asdfas";
        when(bitmapTransformation.getId()).thenReturn(expectedId);

        assertEquals(expectedId, transformation.getId());
    }

    @Test
    public void testAppliesTransformationToBitmapResourceAndReturnsNewGifBitmapResource() {
        int dimens = 123;
        Resource<Bitmap> initial = mock(Resource.class);

        Resource<Bitmap> transformed = mock(Resource.class);
        when(bitmapTransformation.transform(eq(initial), eq(dimens), eq(dimens))).thenReturn(transformed);

        GifBitmap gifBitmap = mock(GifBitmap.class);
        when(gifBitmap.getBitmapResource()).thenReturn(initial);
        Resource<GifBitmap> gifBitmapResource = mock(Resource.class);
        when(gifBitmapResource.get()).thenReturn(gifBitmap);

        assertEquals(transformed, transformation.transform(gifBitmapResource, dimens, dimens).get()
                .getBitmapResource());
    }

    @Test
    public void testReturnsNewGifBitmapResourceIfNoBitmapResource() {
        GifBitmap gifBitmap = mock(GifBitmap.class);
        Resource<GifBitmap> gifBitmapResource = mock(Resource.class);
        when(gifBitmapResource.get()).thenReturn(gifBitmap);

        GifData gifData = mock(GifData.class);
        Resource<GifData> gifDataResource = mock(Resource.class);
        when(gifDataResource.get()).thenReturn(gifData);
        when(gifBitmap.getGifResource()).thenReturn(gifDataResource);

        assertNotSame(gifBitmapResource, transformation.transform(gifBitmapResource, 100, 100));
    }

    @Test
    public void testReturnsGivenResourceIfWrappedTransformationDoesNotTransformBitmapResource() {
        int dimens = 321;
        Resource<Bitmap> initial = mock(Resource.class);
        GifBitmap gifBitmap = mock(GifBitmap.class);
        when(gifBitmap.getBitmapResource()).thenReturn(initial);
        Resource<GifBitmap> gifBitmapResource = mock(Resource.class);
        when(gifBitmapResource.get()).thenReturn(gifBitmap);

        when(bitmapTransformation.transform(eq(initial), eq(dimens), eq(dimens))).thenReturn(initial);

        assertEquals(gifBitmapResource, transformation.transform(gifBitmapResource, dimens, dimens));
    }
}
