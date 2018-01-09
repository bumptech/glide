package com.bumptech.glide.load.engine.bitmap_recycle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.graphics.Bitmap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBitmap;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class AttributeStrategyTest {

  private AttributeStrategy strategy;

  @Before
  public void setUp() throws Exception {
    strategy = new AttributeStrategy();
  }

  @Test
  public void testIGetNullIfNoMatchingBitmapExists() {
    assertNull(strategy.get(100, 100, Bitmap.Config.ARGB_8888));
  }

  @Test
  public void testICanAddAndGetABitmapOfTheSameSizeAndDimensions() {
    Bitmap bitmap = ShadowBitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    strategy.put(bitmap);
    assertEquals(bitmap,
        strategy.get(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888));
  }

  @Test
  public void testICantGetABitmapOfTheSameDimensionsButDifferentConfigs() {
    Bitmap bitmap = ShadowBitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    strategy.put(bitmap);
    assertNull(strategy.get(100, 100, Bitmap.Config.RGB_565));
  }

  @Test
  public void testICantGetABitmapOfTheSameDimensionsAndSizeButDifferentConfigs() {
    Bitmap bitmap = ShadowBitmap.createBitmap(100, 100, Bitmap.Config.ARGB_4444);
    strategy.put(bitmap);
    assertNull(strategy.get(100, 100, Bitmap.Config.RGB_565));
  }

  @Test
  public void testICantGetABitmapOfDifferentWidths() {
    Bitmap bitmap = ShadowBitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    strategy.put(bitmap);
    assertNull(strategy.get(99, 100, Bitmap.Config.ARGB_8888));
  }

  @Test
  public void testICantGetABitmapOfDifferentHeights() {
    Bitmap bitmap = ShadowBitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    strategy.put(bitmap);
    assertNull(strategy.get(100, 99, Bitmap.Config.ARGB_8888));
  }

  @Test
  public void testICantGetABitmapOfDifferentDimensionsButTheSameSize() {
    Bitmap bitmap = ShadowBitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    strategy.put(bitmap);
    assertNull(strategy.get(50, 200, Bitmap.Config.ARGB_8888));
  }

  @Test
  public void testMultipleBitmapsOfDifferentAttributesCanBeAddedAtOnce() {
    Bitmap first = ShadowBitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
    Bitmap second = ShadowBitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    Bitmap third = ShadowBitmap.createBitmap(120, 120, Bitmap.Config.RGB_565);

    strategy.put(first);
    strategy.put(second);
    strategy.put(third);

    assertEquals(first, strategy.get(100, 100, Bitmap.Config.RGB_565));
    assertEquals(second, strategy.get(100, 100, Bitmap.Config.ARGB_8888));
    assertEquals(third, strategy.get(120, 120, Bitmap.Config.RGB_565));
  }

  @Test
  public void testLeastRecentlyUsedAttributeSetIsRemovedFirst() {
    final Bitmap leastRecentlyUsed = ShadowBitmap.createBitmap(100, 100, Bitmap.Config.ALPHA_8);
    final Bitmap other = ShadowBitmap.createBitmap(1000, 1000, Bitmap.Config.RGB_565);
    final Bitmap mostRecentlyUsed = ShadowBitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

    strategy.get(100, 100, Bitmap.Config.ALPHA_8);
    strategy.get(1000, 1000, Bitmap.Config.RGB_565);
    strategy.get(100, 100, Bitmap.Config.ARGB_8888);

    strategy.put(other);
    strategy.put(leastRecentlyUsed);
    strategy.put(mostRecentlyUsed);

    Bitmap removed = strategy.removeLast();
    assertEquals(
        "Expected=" + strategy.logBitmap(leastRecentlyUsed) + " got=" + strategy.logBitmap(removed),
        leastRecentlyUsed, removed);
  }
}
