package com.bumptech.glide.load.resource.bitmap;

import static org.mockito.Mockito.when;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.test.AndroidTestCase;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import org.mockito.Mockito;

/**
 * Emulator tests for Glide transformation utilities.
 */
public class TransformationUtilsTest extends AndroidTestCase {
  private BitmapPool bitmapPool;

  @Override
  public void setUp() throws Exception {
    bitmapPool = new BitmapPoolAdapter();
  }

  public void testRoundedCorners() {
    int width = 20;
    int height = 30;
    Bitmap blueRect = createRect(Color.BLUE, width, height, Bitmap.Config.ARGB_8888);
    Bitmap roundedBlueRect =
        TransformationUtils.roundedCorners(bitmapPool, blueRect, width, height, 5);
    assertBitmapMatches("blue_rect_rounded", roundedBlueRect);
  }

  public void testRoundedCorners_usePool() {
    int width = 20;
    int height = 30;

    Bitmap blueRect = createRect(Color.BLUE, width, height, Bitmap.Config.ARGB_8888);
    Bitmap redRect = createRect(Color.RED, width, height, Bitmap.Config.ARGB_8888);
    BitmapPool mockBitmapPool = Mockito.mock(BitmapPool.class);
    when(mockBitmapPool.get(width, height, Bitmap.Config.ARGB_8888)).thenReturn(redRect);

    Bitmap roundedBlueRect =
        TransformationUtils.roundedCorners(mockBitmapPool, blueRect, width, height, 5);
    assertBitmapMatches("blue_rect_rounded", roundedBlueRect);
    assertSame("Did not reuse provided Bitmap.", redRect, roundedBlueRect);
  }

  public void testRoundedCorners_overRounded() {
    int width = 40;
    int height = 20;
    Bitmap blueRect = createRect(Color.BLUE, width, height, Bitmap.Config.RGB_565);
    Bitmap roundedBlueRect =
        TransformationUtils.roundedCorners(bitmapPool, blueRect, width, height, 20);
    assertBitmapMatches("blue_rect_over_rounded", roundedBlueRect);
  }

  private Bitmap createRect(int color, int width, int height, Bitmap.Config config) {
    final Bitmap result = Bitmap.createBitmap(width, height, config);
    Canvas canvas = new Canvas(result);
    canvas.drawColor(color);
    return result;
  }

  private void assertBitmapMatches(String resourceName, Bitmap actual) {
    Resources res = getContext().getResources();
    int resId = res.getIdentifier(resourceName, "drawable", "com.bumptech.glide");
    assertTrue("Cannot find drawable for resource name: " + resourceName, resId > 0);

    Bitmap expected = BitmapFactory.decodeResource(res, resId);
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
