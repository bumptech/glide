package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.graphics.Bitmap;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class GlideBitmapDrawableResourceTest {
    private GlideBitmapDrawableResourceHarness harness;

    @Before
    public void setUp() {
        harness = new GlideBitmapDrawableResourceHarness();
    }

    @Test
    public void testReturnsGivenBitmapFromGet() {
        assertEquals(harness.bitmap, harness.create().get().getBitmap());
    }

    @Test
    public void testReturnsGivenDrawableOnFirstGet() {
        GlideBitmapDrawableResource resource = harness.create();
        assertEquals(harness.drawable, resource.get());
    }

    @Test
    public void testReturnsDifferentDrawableEachTime() {
        GlideBitmapDrawableResource resource = harness.create();
        GlideBitmapDrawable first = resource.get();
        GlideBitmapDrawable second = resource.get();

        assertNotSame(first, second);
    }

    @Test
    public void testReturnsSizeFromGivenBitmap() {
        assertEquals(harness.bitmap.getHeight() * harness.bitmap.getRowBytes(), harness.create().getSize());
    }

    @Test
    public void testBitmapIsReturnedToPoolOnRecycle() {
        harness.create().recycle();

        verify(harness.bitmapPool).put(eq(harness.bitmap));
    }

    private static class GlideBitmapDrawableResourceHarness {
        BitmapPool bitmapPool = mock(BitmapPool.class);
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        GlideBitmapDrawable drawable = new GlideBitmapDrawable(Robolectric.application.getResources(),  bitmap);

        public GlideBitmapDrawableResource create() {
            return new GlideBitmapDrawableResource(drawable, bitmapPool);
        }
    }

}