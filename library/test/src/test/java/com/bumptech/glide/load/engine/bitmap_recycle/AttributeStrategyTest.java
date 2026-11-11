package com.bumptech.glide.load.engine.bitmap_recycle;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.graphics.Bitmap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class AttributeStrategyTest {

  private AttributeStrategy strategy;

  @Before
  public void setUp() throws Exception {
    strategy = new AttributeStrategy();
  }

  @Test
  public void iGetNullIfNoMatchingBitmapExists() {
    assertNull(strategy.get(100, 100, Bitmap.Config.ARGB_8888));
  }

  @Test
  public void iCanAddAndGetABitmapOfTheSameSizeAndDimensions() {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    strategy.put(bitmap);
    assertEquals(
        bitmap, strategy.get(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888));
  }

  @Test
  public void iCantGetABitmapOfTheSameDimensionsButDifferentConfigs() {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    strategy.put(bitmap);
    assertNull(strategy.get(100, 100, Bitmap.Config.RGB_565));
  }

  @Test
  public void iCantGetABitmapOfTheSameDimensionsAndSizeButDifferentConfigs() {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_4444);
    strategy.put(bitmap);
    assertNull(strategy.get(100, 100, Bitmap.Config.RGB_565));
  }

  @Test
  public void iCantGetABitmapOfDifferentWidths() {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    strategy.put(bitmap);
    assertNull(strategy.get(99, 100, Bitmap.Config.ARGB_8888));
  }

  @Test
  public void iCantGetABitmapOfDifferentHeights() {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    strategy.put(bitmap);
    assertNull(strategy.get(100, 99, Bitmap.Config.ARGB_8888));
  }

  @Test
  public void iCantGetABitmapOfDifferentDimensionsButTheSameSize() {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    strategy.put(bitmap);
    assertNull(strategy.get(50, 200, Bitmap.Config.ARGB_8888));
  }

  @Test
  public void multipleBitmapsOfDifferentAttributesCanBeAddedAtOnce() {
    Bitmap first = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
    Bitmap second = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    Bitmap third = Bitmap.createBitmap(120, 120, Bitmap.Config.RGB_565);

    strategy.put(first);
    strategy.put(second);
    strategy.put(third);

    assertEquals(first, strategy.get(100, 100, Bitmap.Config.RGB_565));
    assertEquals(second, strategy.get(100, 100, Bitmap.Config.ARGB_8888));
    assertEquals(third, strategy.get(120, 120, Bitmap.Config.RGB_565));
  }

  @Test
  public void leastRecentlyUsedAttributeSetIsRemovedFirst() {
    final Bitmap leastRecentlyUsed = Bitmap.createBitmap(100, 100, Bitmap.Config.ALPHA_8);
    final Bitmap other = Bitmap.createBitmap(1000, 1000, Bitmap.Config.RGB_565);
    final Bitmap mostRecentlyUsed = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

    strategy.get(100, 100, Bitmap.Config.ALPHA_8);
    strategy.get(1000, 1000, Bitmap.Config.RGB_565);
    strategy.get(100, 100, Bitmap.Config.ARGB_8888);

    strategy.put(other);
    strategy.put(leastRecentlyUsed);
    strategy.put(mostRecentlyUsed);

    Bitmap removed = strategy.removeLast();
    assertEquals(
        "Expected=" + strategy.logBitmap(leastRecentlyUsed) + " got=" + strategy.logBitmap(removed),
        leastRecentlyUsed,
        removed);
  }
}
