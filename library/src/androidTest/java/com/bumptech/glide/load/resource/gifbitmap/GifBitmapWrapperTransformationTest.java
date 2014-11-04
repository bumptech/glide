package com.bumptech.glide.load.resource.gifbitmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.gif.GifDrawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class GifBitmapWrapperTransformationTest {
    private Transformation<Bitmap> bitmapTransformation;
    private Transformation<GifDrawable> gifTransformation;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        bitmapTransformation = mock(Transformation.class);
        gifTransformation = mock(Transformation.class);
    }

    private class BitmapResourceHarness {
        Resource<Bitmap> bitmapResource = mock(Resource.class);
        GifBitmapWrapper gifBitmapWrapper = mock(GifBitmapWrapper.class);
        Resource<GifBitmapWrapper> resource = mock(Resource.class);
        GifBitmapWrapperTransformation transformation = new GifBitmapWrapperTransformation(
                mock(BitmapPool.class), bitmapTransformation);
        int width = 123;
        int height = 456;

        public BitmapResourceHarness() {
            when(gifBitmapWrapper.getBitmapResource()).thenReturn(bitmapResource);
            when(resource.get()).thenReturn(gifBitmapWrapper);
        }
    }

    private class GifResourceHarness {
        GifDrawable gifDrawable = mock(GifDrawable.class);
        Resource<GifDrawable> gifResource = mock(Resource.class);
        GifBitmapWrapper gifBitmapWrapper = mock(GifBitmapWrapper.class);
        Resource<GifBitmapWrapper> resource = mock(Resource.class);
        GifBitmapWrapperTransformation transformation = new GifBitmapWrapperTransformation(null, gifTransformation);
        int width = 123;
        int height = 456;

        public GifResourceHarness() {
            when(gifResource.get()).thenReturn(gifDrawable);
            when(gifBitmapWrapper.getGifResource()).thenReturn(gifResource);
            when(resource.get()).thenReturn(gifBitmapWrapper);
        }
    }

    @Test
    public void testHasValidId() {
        String expectedId = "testID";
        when(bitmapTransformation.getId()).thenReturn(expectedId);
        BitmapPool pool = mock(BitmapPool.class);

        GifBitmapWrapperTransformation transformation = new GifBitmapWrapperTransformation(pool, bitmapTransformation);

        assertEquals(expectedId, transformation.getId());
    }

    @Test
    public void testAppliesBitmapTransformationIfBitmapTransformationIsGivenAndResourceHasBitmapResource() {
        BitmapResourceHarness harness = new BitmapResourceHarness();

        Resource<Bitmap> transformedBitmapResource = mock(Resource.class);
        when(bitmapTransformation.transform(eq(harness.bitmapResource), eq(harness.width), eq(harness.height)))
                .thenReturn(transformedBitmapResource);
        Resource<GifBitmapWrapper> transformed = harness.transformation.transform(harness.resource, harness.width,
                harness.height);

        assertNotSame(harness.resource, transformed);
        assertEquals(transformedBitmapResource, transformed.get().getBitmapResource());
    }

    @Test
    public void testReturnsOriginalResourceIfTransformationDoesNotTransformGivenBitmapResource() {
        BitmapResourceHarness harness = new BitmapResourceHarness();

        when(bitmapTransformation.transform(eq(harness.bitmapResource), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.bitmapResource);
        Resource<GifBitmapWrapper> transformed = harness.transformation.transform(harness.resource, harness.width,
                harness.height);

        assertSame(harness.resource, transformed);
    }

    @Test
    public void testReturnsOriginalResourceIfBitmapTransformationIsGivenButResourceHasNoBitmapResource() {
        BitmapResourceHarness harness = new BitmapResourceHarness();
        when(harness.gifBitmapWrapper.getBitmapResource()).thenReturn(null);

        Resource<GifBitmapWrapper> transformed = harness.transformation.transform(harness.resource, harness.width,
                harness.height);

        assertSame(harness.resource, transformed);
    }

    @Test
    public void testAppliesGifTransformationIfGifTransformationGivenAndResourceHasGifResource() {
        GifResourceHarness harness = new GifResourceHarness();
        Resource<GifDrawable> transformedGifResource = mock(Resource.class);
        when(gifTransformation.transform(eq(harness.gifResource), eq(harness.width), eq(harness.height)))
                .thenReturn(transformedGifResource);
        Resource<GifBitmapWrapper> transformed = harness.transformation.transform(harness.resource, harness.width,
                harness.height);

        assertNotSame(harness.resource, transformed);
        assertEquals(transformedGifResource, transformed.get().getGifResource());
    }

    @Test
    public void testReturnsOriginalresourceIfTransformationDoesNotTransformGivenGifResource() {
        GifResourceHarness harness = new GifResourceHarness();
        when(gifTransformation.transform(eq(harness.gifResource), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.gifResource);

        Resource<GifBitmapWrapper> transformed = harness.transformation.transform(harness.resource, harness.width,
                harness.height);

        assertSame(harness.resource, transformed);
    }

    @Test
    public void testReturnsOriginalResourceIfGifTransformationIsGivenButResourceHasNoGifResource() {
        GifResourceHarness harness = new GifResourceHarness();
        when(harness.gifBitmapWrapper.getGifResource()).thenReturn(null);

        Resource<GifBitmapWrapper> transformed = harness.transformation.transform(harness.resource, harness.width,
                harness.height);

        assertSame(harness.resource, transformed);
    }
}

