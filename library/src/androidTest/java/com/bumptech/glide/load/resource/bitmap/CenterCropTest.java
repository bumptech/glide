package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.tests.Util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class CenterCropTest {
    private CenterCropHarness harness;

    @Before
    public void setUp() {
        harness = new CenterCropHarness();
    }

    @Test
    public void testDoesNotPutNullBitmapAcquiredFromPool() {
        when(harness.pool.get(anyInt(), anyInt(), any(Bitmap.Config.class))).thenReturn(null);

        harness.centerCrop.transform(harness.resource, 100, 100);

        verify(harness.pool, never()).put(any(Bitmap.class));
    }

    @Test
    public void testReturnsGivenResourceIfMatchesSizeExactly() {
        Resource<Bitmap> result = harness.centerCrop.transform(harness.resource, harness.bitmapWidth,
                harness.bitmapHeight);

        assertEquals(harness.resource, result);
    }

    @Test
    public void testDoesNotRecycleGivenResourceIfMatchesSizeExactly() {
        harness.centerCrop.transform(harness.resource, harness.bitmapWidth,
                harness.bitmapHeight);

        verify(harness.resource, never()).recycle();
    }

    @Test
    public void testDoesPutNonNullBitmapAcquiredFromPoolWhenUnused() {
        Bitmap fromPool = Bitmap.createBitmap(harness.bitmapWidth, harness.bitmapHeight, Bitmap.Config.ARGB_8888);
        when(harness.pool.get(anyInt(), anyInt(), any(Bitmap.Config.class)))
                .thenReturn(fromPool);

        harness.centerCrop.transform(harness.resource, harness.bitmapWidth, harness.bitmapHeight);

        verify(harness.pool).put(eq(fromPool));
    }

    @Test
    public void testDoesNotRecycleGivenResource() {
        harness.centerCrop.transform(harness.resource, 50, 50);

        verify(harness.resource, never()).recycle();
    }

    @Test
    public void testAsksBitmapPoolForArgb8888IfInConfigIsNull() {
        Robolectric.shadowOf(harness.bitmap).setConfig(null);

        harness.centerCrop.transform(harness.resource, 10, 10);

        verify(harness.pool).get(anyInt(), anyInt(), eq(Bitmap.Config.ARGB_8888));
        verify(harness.pool, never()).get(anyInt(), anyInt(), (Bitmap.Config) isNull());
    }

    @Test
    public void testReturnsBitmapWithExactlyGivenDimensionsIfBitmapIsLargerThanTarget() {
        int expectedWidth = 75;
        int expectedHeight = 74;

        Resource<Bitmap> resource = mock(Resource.class);
        for (int[] dimens : new int[][] { new int[] { 800, 200}, new int[] { 450, 100 }, new int[] { 78, 78 }}) {
            Bitmap toTransform = Bitmap.createBitmap(dimens[0], dimens[1], Bitmap.Config.ARGB_4444);
            when(resource.get()).thenReturn(toTransform);

            Resource<Bitmap> result = harness.centerCrop.transform(resource, expectedWidth, expectedHeight);
            Bitmap transformed = result.get();
            assertEquals(expectedWidth, transformed.getWidth());
            assertEquals(expectedHeight, transformed.getHeight());
        }
    }

    @Test
    public void testReturnsBitmapWithExactlyGivenDimensionsIfBitmapIsSmallerThanTarget() {
        int expectedWidth = 100;
        int expectedHeight = 100;

        Resource<Bitmap> resource = mock(Resource.class);
        for (int[] dimens : new int[][] { new int[] { 50, 90}, new int[] { 150, 2 }, new int[] { 78, 78 }}) {
            Bitmap toTransform = Bitmap.createBitmap(dimens[0], dimens[1], Bitmap.Config.ARGB_4444);
            when(resource.get()).thenReturn(toTransform);

            Resource<Bitmap> result = harness.centerCrop.transform(resource, expectedWidth, expectedHeight);
            Bitmap transformed = result.get();
            assertEquals(expectedWidth, transformed.getWidth());
            assertEquals(expectedHeight, transformed.getHeight());
        }
    }

    @Test
    public void testHasValidId() {
        Util.assertClassHasValidId(CenterCrop.class, harness.centerCrop.getId());
    }

    private static class CenterCropHarness {
        int bitmapWidth = 100;
        int bitmapHeight = 100;
        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        BitmapResource resource = mock(BitmapResource.class);
        BitmapPool pool = mock(BitmapPool.class);

        CenterCrop centerCrop = new CenterCrop(pool);

        public CenterCropHarness() {
            when(resource.get()).thenReturn(bitmap);
        }
    }
}
