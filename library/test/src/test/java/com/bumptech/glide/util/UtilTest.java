package com.bumptech.glide.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class UtilTest {

  @Test
  public void testReturnsCorrectBitmapSizeForDifferentDimensions() {
    int width = 100;
    int height = 100;
    Bitmap.Config config = Bitmap.Config.ARGB_8888;

    int initialSize = Util.getBitmapByteSize(width, height, config);
    int sizeOne = Util.getBitmapByteSize(width * 2, height, config);
    int sizeTwo = Util.getBitmapByteSize(width, height * 2, config);

    assertEquals(4 * width * height, initialSize);
    assertEquals(2 * initialSize, sizeOne);
    assertEquals(2 * initialSize, sizeTwo);
  }

  @Test
  public void testReturnsCorrectBitmapSizeForAlpha8Bitmap() {
    int width = 110;
    int height = 43;

    int size = Util.getBitmapByteSize(width, height, Bitmap.Config.ALPHA_8);
    assertEquals(width * height, size);
  }

  @Test
  public void testReturnsCorrectBitmapSizeForRgb565() {
    int width = 34;
    int height = 1444;

    int size = Util.getBitmapByteSize(width, height, Bitmap.Config.RGB_565);
    assertEquals(width * height * 2, size);
  }

  @Test
  public void testReturnsCorrectBitmapSizeForARGB4444() {
    int width = 4454;
    int height = 1235;

    int size = Util.getBitmapByteSize(width, height, Bitmap.Config.ARGB_4444);
    assertEquals(width * height * 2, size);
  }

  @Test
  public void testReturnsCorrectBitmapSizeForARGB8888() {
    int width = 943;
    int height = 3584;

    int size = Util.getBitmapByteSize(width, height, Bitmap.Config.ARGB_8888);
    assertEquals(width * height * 4, size);
  }

  @Test
  public void testReturnsLargestSizeForNullConfig() {
    int width = 999;
    int height = 41324;
    int size = Util.getBitmapByteSize(width, height, null);
    assertEquals(width * height * 4, size);
  }

  @Test
  public void getBitmapByteSize_withRGBA_F16_returnsCorrectSize() {
    int width = 100;
    int height = 200;
    assertThat(Util.getBitmapByteSize(width, height, Bitmap.Config.RGBA_F16))
        .isEqualTo(width * height * 8);
  }
}
