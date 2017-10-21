package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.test.ResourceIds;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Emulator tests for Glide transformation utilities.
 */
@RunWith(AndroidJUnit4.class)
public class TransformationUtilsTest {
  private BitmapPool bitmapPool;
  private Context context;

  @Before
  public void setUp() throws Exception {
    bitmapPool = new BitmapPoolAdapter();
    context = InstrumentationRegistry.getContext();
    // TODO: Add Android API specific resources that work > API 16.
    assumeTrue(Build.VERSION.SDK_INT <= VERSION_CODES.JELLY_BEAN);
  }

  @Test
  public void testRoundedCorners() {
    int width = 20;
    int height = 30;
    Bitmap blueRect = createRect(Color.BLUE, width, height, Bitmap.Config.ARGB_8888);
    Bitmap roundedBlueRect =
        TransformationUtils.roundedCorners(bitmapPool, blueRect, 5);
    assertBitmapMatches(ResourceIds.raw.blue_rect_rounded, roundedBlueRect);
  }

  @Test
  public void testRoundedCorners_usePool() {
    int width = 20;
    int height = 30;

    Bitmap blueRect = createRect(Color.BLUE, width, height, Bitmap.Config.ARGB_8888);
    Bitmap redRect = createRect(Color.RED, width, height, Bitmap.Config.ARGB_8888);
    BitmapPool mockBitmapPool = mock(BitmapPool.class);
    when(mockBitmapPool.get(width, height, Config.ARGB_8888)).thenReturn(redRect);

    Bitmap roundedBlueRect =
        TransformationUtils.roundedCorners(mockBitmapPool, blueRect, 5);
    assertBitmapMatches(ResourceIds.raw.blue_rect_rounded, roundedBlueRect);
    assertThat(roundedBlueRect).isEqualTo(redRect);
  }

  @Test
  public void testRoundedCorners_overRounded() {
    int width = 40;
    int height = 20;
    Bitmap blueRect = createRect(Color.BLUE, width, height, Bitmap.Config.RGB_565);
    Bitmap roundedBlueRect =
        TransformationUtils.roundedCorners(bitmapPool, blueRect, 20);
    assertBitmapMatches(ResourceIds.raw.blue_rect_over_rounded, roundedBlueRect);
  }

  private Bitmap createRect(int color, int width, int height, Bitmap.Config config) {
    final Bitmap result = Bitmap.createBitmap(width, height, config);
    Canvas canvas = new Canvas(result);
    canvas.drawColor(color);
    return result;
  }

  private void assertBitmapMatches(int resId, Bitmap actual) {
    Resources res = context.getResources();
    // Avoid default density scaling when decoding the expected Bitmap.
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inScaled = false;
    Bitmap expected = BitmapFactory.decodeResource(res, resId, options);
    assertPixelDataMatches(expected, actual);
  }

  /**
   * TODO(user): Pull this out into a helper library and add tests for it. Would also be good
   * to get a tool for updating expected bitmaps on functionality changes and new tests.
   */
  private void assertPixelDataMatches(Bitmap expected, Bitmap actual) {
    assertEquals(expected.getWidth(), actual.getWidth());
    assertEquals(expected.getHeight(), actual.getHeight());

    for (int y = 0; y < expected.getHeight(); y++) {
      for (int x = 0; x < expected.getWidth(); x++) {
        assertEquals(expected.getPixel(x, y), actual.getPixel(x, y));
      }
    }
  }
}
