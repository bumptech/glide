package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
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
        Resource<Bitmap> result = harness.centerCrop.transform(harness.resource, harness.bitmapWidth,
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
