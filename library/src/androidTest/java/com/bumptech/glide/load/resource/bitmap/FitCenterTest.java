package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class FitCenterTest {
    private FitCenterHarness harness;

    @Before
    public void setUp() {
        harness = new FitCenterHarness();
    }

    @Test
    public void testDoesNotPutNullBitmapAcquiredFromPool() {
        when(harness.pool.get(anyInt(), anyInt(), any(Bitmap.Config.class))).thenReturn(null);

        harness.fitCenter.transform(harness.resource, 100, 100);

        verify(harness.pool, never()).put(any(Bitmap.class));
    }

    @Test
    public void testReturnsGivenResourceIfMatchesSizeExactly() {
        Resource<Bitmap> result = harness.fitCenter.transform(harness.resource, harness.bitmapWidth,
                harness.bitmapHeight);

        assertEquals(harness.resource, result);
    }

    @Test
    public void testDoesNotRecycleGivenResourceIfMatchesSizeExactly() {
        harness.fitCenter.transform(harness.resource, harness.bitmapWidth,
                harness.bitmapHeight);

        verify(harness.resource, never()).recycle();
    }

    @Test
    public void testDoesNotRecycleGivenResource() {
        harness.fitCenter.transform(harness.resource, 50, 50);

        verify(harness.resource, never()).recycle();
    }

    @Test
    public void testHasValidId() {
        Util.assertClassHasValidId(FitCenter.class, harness.fitCenter.getId());
    }


    private static class FitCenterHarness {
        int bitmapWidth = 100;
        int bitmapHeight = 100;
        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        BitmapResource resource = mock(BitmapResource.class);
        BitmapPool pool = mock(BitmapPool.class);

        FitCenter fitCenter = new FitCenter(pool);

        public FitCenterHarness() {
            when(resource.get()).thenReturn(bitmap);
        }
    }
}
