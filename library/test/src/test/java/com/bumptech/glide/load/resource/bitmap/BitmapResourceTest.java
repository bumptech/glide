package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.graphics.Bitmap;
import android.os.Build;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.tests.Util;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// TODO: add a test for bitmap size using getAllocationByteSize when robolectric supports kitkat.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
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
    Util.setSdkVersionInt(currentBuildVersion);
  }

  @Test
  public void testCanGetBitmap() {
    assertEquals(harness.bitmap, harness.resource.get());
  }

  @Test
  public void testSizeIsBasedOnDimensPreKitKat() {
    Util.setSdkVersionInt(18);
    assertEquals(
        harness.bitmap.getWidth() * harness.bitmap.getHeight() * 4, harness.resource.getSize());
  }

  @Test
  public void testPutsBitmapInPoolOnRecycle() {
    harness.resource.recycle();

    verify(harness.bitmapPool).put(eq(harness.bitmap));
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsIfBitmapIsNull() {
    new BitmapResource(null, mock(BitmapPool.class));
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsIfBitmapPoolIsNull() {
    new BitmapResource(Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565), null);
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsIfBitmapAndBitmapPoolAreNull() {
    new BitmapResource(null, null);
  }

  private static class BitmapResourceHarness {
    final Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    final BitmapPool bitmapPool = mock(BitmapPool.class);
    final BitmapResource resource = new BitmapResource(bitmap, bitmapPool);
  }
}
