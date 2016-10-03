package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class BitmapDrawableResourceTest {
  private BitmapDrawableResourceHarness harness;

  @Before
  public void setUp() {
    harness = new BitmapDrawableResourceHarness();
  }

  @Test
  public void testReturnsGivenBitmapFromGet() {
    assertEquals(harness.bitmap, harness.create().get().getBitmap());
  }

  @Test
  public void testReturnsDifferentDrawableEachTime() {
    BitmapDrawableResource resource = harness.create();
    BitmapDrawable first = resource.get();
    BitmapDrawable second = resource.get();

    assertNotSame(first, second);
  }

  @Test
  public void testReturnsSizeFromGivenBitmap() {
    assertEquals(harness.bitmap.getHeight() * harness.bitmap.getRowBytes(),
        harness.create().getSize());
  }

  @Test
  public void testBitmapIsReturnedToPoolOnRecycle() {
    harness.create().recycle();

    verify(harness.bitmapPool).put(eq(harness.bitmap));
  }

  private static class BitmapDrawableResourceHarness {
    BitmapPool bitmapPool = mock(BitmapPool.class);
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

    public BitmapDrawableResource create() {
      return new BitmapDrawableResource(
          new BitmapDrawable(RuntimeEnvironment.application.getResources(), bitmap), bitmapPool);
    }
  }
}
