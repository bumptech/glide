package com.bumptech.glide.load.resource.bitmap;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.graphics.Bitmap;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class BitmapResourceTest {
  private BitmapResourceHarness harness;

  @Before
  public void setUp() {
    harness = new BitmapResourceHarness();
  }

  @Test
  public void canGetBitmap() {
    assertEquals(harness.bitmap, harness.resource.get());
  }

  @Test
  public void canGetSize() {
    assertEquals(harness.bitmap.getAllocationByteCount(), harness.resource.getSize());
  }

  @Test
  public void putsBitmapInPoolOnRecycle() {
    harness.resource.recycle();

    verify(harness.bitmapPool).put(eq(harness.bitmap));
  }

  @Test(expected = NullPointerException.class)
  public void throwsIfBitmapIsNull() {
    new BitmapResource(null, mock(BitmapPool.class));
  }

  @Test(expected = NullPointerException.class)
  public void throwsIfBitmapPoolIsNull() {
    new BitmapResource(Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565), null);
  }

  @Test(expected = NullPointerException.class)
  public void throwsIfBitmapAndBitmapPoolAreNull() {
    new BitmapResource(null, null);
  }

  private static class BitmapResourceHarness {
    final Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    final BitmapPool bitmapPool = mock(BitmapPool.class);
    final BitmapResource resource = new BitmapResource(bitmap, bitmapPool);
  }
}
