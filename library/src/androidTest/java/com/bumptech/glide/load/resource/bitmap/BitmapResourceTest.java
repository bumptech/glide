package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.os.Build;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//TODO: add a test for bitmap size using getAllocationByteSize when robolectric supports kitkat.
@RunWith(RobolectricTestRunner.class)
public class BitmapResourceTest {
    private int currentBuildVersion;
    private BitmapResourceHarness harness;

    @Before
    public void setUp() {
        currentBuildVersion = Build.VERSION.SDK_INT;
        harness = new BitmapResourceHarness();
    }

    @After
    public void tearDown() {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", currentBuildVersion);
    }

    @Test
    public void testCanGetBitmap() {
        assertEquals(harness.bitmap, harness.resource.get());
    }

    @Test
    public void testSizeIsBasedOnDimensPreKitKat() {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", 18);
        assertEquals(harness.bitmap.getWidth() * harness.bitmap.getHeight() * 4, harness.resource.getSize());
    }

    @Test
    public void testPutsBitmapInPoolOnRecycle() {
        harness.resource.recycle();

        verify(harness.bitmapPool).put(eq(harness.bitmap));
    }

    @Test
    public void testBitmapIsNotRecycledIfAcceptedByPool() {
        when(harness.bitmapPool.put(eq(harness.bitmap))).thenReturn(true);

        harness.resource.recycle();

        assertFalse(harness.bitmap.isRecycled());
    }

    @Test
    public void testRecyclesBitmapIfRejectedByPool() {
        when(harness.bitmapPool.put(eq(harness.bitmap))).thenReturn(false);

        harness.resource.recycle();

        assertTrue(harness.bitmap.isRecycled());
    }

    private static class BitmapResourceHarness {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        BitmapPool bitmapPool = mock(BitmapPool.class);
        BitmapResource resource = new BitmapResource(bitmap, bitmapPool);
    }
}
