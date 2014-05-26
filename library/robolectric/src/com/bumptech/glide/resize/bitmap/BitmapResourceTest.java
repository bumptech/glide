package com.bumptech.glide.resize.bitmap;

import android.graphics.Bitmap;
import android.os.Build;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;

//TODO: add a test for bitmap size using getAllocationByteSize when robolectric supports kitkat.
@RunWith(RobolectricTestRunner.class)
public class BitmapResourceTest {
    private int currentBuildVersion;

    @Before
    public void setUp() {
        currentBuildVersion = Build.VERSION.SDK_INT;
    }

    @After
    public void tearDown() {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", currentBuildVersion);
    }


    @Test
    public void testCanGetBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        BitmapResource resource = new BitmapResource(bitmap);

        assertEquals(bitmap, resource.get());
    }

    @Test
    public void testSizeIsBasedOnDimensPreKitKat() {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", 18);
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        BitmapResource resource = new BitmapResource(bitmap);

        assertEquals(100 * 100 * 4, resource.getSize());
    }
}
