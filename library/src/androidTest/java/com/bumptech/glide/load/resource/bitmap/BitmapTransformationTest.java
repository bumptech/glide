package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class BitmapTransformationTest {

    private BitmapPool bitmapPool;

    @Before
    public void setUp() {
        bitmapPool = mock(BitmapPool.class);
    }

    @Test
    public void testReturnsGivenResourceWhenBitmapNotTransformed() {
        BitmapTransformation transformation = new BitmapTransformation(bitmapPool) {
            @Override
            protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
                return toTransform;
            }

            @Override
            public String getId() {
                return null;
            }
        };

        Resource<Bitmap> resource = mock(Resource.class);
        when(resource.get()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_4444));
        assertEquals(resource, transformation.transform(resource, 1, 1));
    }

    @Test
    public void testReturnsNewResourceWhenBitmapTransformed() {
        final Bitmap toTransform = Bitmap.createBitmap(1, 2, Bitmap.Config.RGB_565);
        final Bitmap transformed = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_4444);
        BitmapTransformation transformation = new BitmapTransformation(bitmapPool) {
            @Override
            protected Bitmap transform(BitmapPool pool, Bitmap bitmap, int outWidth, int outHeight) {
                return transformed;
            }

            @Override
            public String getId() {
                return null;
            }
        };

        Resource<Bitmap> resource = mock(Resource.class);
        when(resource.get()).thenReturn(toTransform);

        assertNotSame(resource, transformation.transform(resource, 100, 100));
    }

    @Test
    public void testPassesGivenArgumentsToTransform() {
        final int expectedWidth = 13;
        final int expectedHeight = 148;
        final Bitmap expected = Bitmap.createBitmap(223, 4123, Bitmap.Config.RGB_565);
        BitmapTransformation transformation = new BitmapTransformation(bitmapPool) {
            @Override
            protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
                assertEquals(bitmapPool, pool);
                assertEquals(expected, toTransform);
                assertEquals(expectedWidth, outWidth);
                assertEquals(expectedHeight, outHeight);
                return expected;
            }

            @Override
            public String getId() {
                return null;
            }
        };
        Resource<Bitmap> resource = mock(Resource.class);
        when(resource.get()).thenReturn(expected);
        transformation.transform(resource, expectedWidth, expectedHeight);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfGivenInvalidWidth() {
        BitmapTransformation transformation = new BitmapTransformation(bitmapPool) {

            @Override
            protected Bitmap transform(BitmapPool bitmapPool, Bitmap toTransform, int outWidth, int outHeight) {
                return null;
            }

            @Override
            public String getId() {
                return null;
            }

        };
        transformation.transform(mock(Resource.class), -1, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfGivenInvalidHeight() {
        BitmapTransformation transformation = new BitmapTransformation(bitmapPool) {

            @Override
            protected Bitmap transform(BitmapPool bitmapPool, Bitmap toTransform, int outWidth, int outHeight) {
                return null;
            }

            @Override
            public String getId() {
                return null;
            }

        };
        transformation.transform(mock(Resource.class), 100, -1);
    }

    @Test
    public void testReturnsNullIfTransformReturnsNull() {
        BitmapTransformation transform = new BitmapTransformation(bitmapPool) {

            @Override
            public String getId() {
                return null;
            }

            @Override
            protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
                return null;
            }
        };

        Resource<Bitmap> resource = mock(Resource.class);
        when(resource.get()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565));
        assertNull(transform.transform(resource, 100, 100));
    }
}