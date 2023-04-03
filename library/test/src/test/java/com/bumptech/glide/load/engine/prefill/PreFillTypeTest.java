package com.bumptech.glide.load.engine.prefill;

import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class PreFillTypeTest {

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfSizeIsZero() {
    new PreFillType.Builder(0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfWidthIsZero() {
    new PreFillType.Builder(0, 100);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfHeightIsZero() {
    new PreFillType.Builder(100, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfWeightIsZero() {
    new PreFillType.Builder(100).setWeight(0);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorThrowsIfConfigIsNull() {
    new PreFillType(100, 100, null, 1);
  }

  @Test
  public void testGetWidthReturnsGivenWidth() {
    int width = 500;
    assertEquals(width, new PreFillType(width, 100, Bitmap.Config.ARGB_4444, 1).getWidth());
  }

  @Test
  public void testGetHeightReturnsGivenHeight() {
    int height = 123;
    assertEquals(height, new PreFillType(100, height, Bitmap.Config.ARGB_4444, 1).getHeight());
  }

  @Test
  public void testGetConfigReturnsGivenConfig() {
    Bitmap.Config config = Bitmap.Config.ARGB_8888;
    assertEquals(config, new PreFillType(100, 100, config, 1).getConfig());
  }

  @Test
  public void testGetWeightReturnsGivenWeight() {
    int weight = 400;
    assertEquals(weight, new PreFillType(100, 100, Bitmap.Config.ARGB_4444, weight).getWeight());
  }

  @Test
  public void testEquality() {
    new EqualsTester()
        .addEqualityGroup(
            new PreFillType(100, 100, Bitmap.Config.ARGB_4444, 1),
            new PreFillType(100, 100, Bitmap.Config.ARGB_4444, 1))
        .addEqualityGroup(new PreFillType(200, 100, Bitmap.Config.ARGB_4444, 1))
        .addEqualityGroup(new PreFillType(100, 200, Bitmap.Config.ARGB_4444, 1))
        .addEqualityGroup(new PreFillType(100, 100, Bitmap.Config.ARGB_8888, 1))
        .addEqualityGroup(new PreFillType(100, 100, Bitmap.Config.ARGB_4444, 2))
        .testEquals();
  }
}
