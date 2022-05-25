package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.tests.KeyTester;
import com.bumptech.glide.tests.Util;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class CircleCropTest {
  @Rule public final KeyTester keyTester = new KeyTester();
  @Mock private BitmapPool bitmapPool;

  private CircleCrop circleCrop;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(bitmapPool.get(anyInt(), anyInt(), any(Bitmap.Config.class)))
        .thenAnswer(new Util.CreateBitmap());
    Context context = ApplicationProvider.getApplicationContext();
    Glide.init(context, new GlideBuilder().setBitmapPool(bitmapPool));
    circleCrop = new CircleCrop();
  }

  @After
  public void tearDown() {
    Glide.tearDown();
  }

  @Test
  public void testTransform_withSquare() {
    Bitmap redSquare = createSolidRedBitmap(50, 50);
    Bitmap result = circleCrop.transform(bitmapPool, redSquare, 50, 50);
    Bitmap expected = createBitmapWithRedCircle(50, 50);

    assertSamePixels(expected, result);
  }

  @Test
  public void testTransform_reusesBitmap() {
    Bitmap toReuse = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
    when(bitmapPool.get(50, 50, Bitmap.Config.ARGB_8888)).thenReturn(toReuse);

    Bitmap redSquare = createSolidRedBitmap(50, 50);
    Bitmap result = circleCrop.transform(bitmapPool, redSquare, 50, 50);

    assertEquals(toReuse, result);
  }

  @Test
  public void testTransform_withWideRectangle() {
    Bitmap redWideRectangle = createSolidRedBitmap(100, 50);
    Bitmap result = circleCrop.transform(bitmapPool, redWideRectangle, 80, 50);
    Bitmap expected = createBitmapWithRedCircle(80, 50);

    assertSamePixels(expected, result);
  }

  @Test
  public void testTransform_withNarrowRectangle() {
    Bitmap redNarrowRectangle = createSolidRedBitmap(20, 50);
    Bitmap result = circleCrop.transform(bitmapPool, redNarrowRectangle, 40, 80);
    Bitmap expected = createBitmapWithRedCircle(40, 80);

    assertSamePixels(expected, result);
  }

  @Test
  public void testEquals() {
    keyTester
        .addEquivalenceGroup(circleCrop, new CircleCrop())
        .addEquivalenceGroup(mock(Transformation.class))
        .addRegressionTest(
            new CircleCrop(), "1442365bcc658f89310e39844ef4be58f4b16e52c283254e5a458020f56acb90")
        .test();
  }

  private void assertSamePixels(Bitmap expected, Bitmap actual) {
    assertEquals(expected.getWidth(), actual.getWidth());
    assertEquals(expected.getHeight(), actual.getHeight());
    assertEquals(expected.getConfig(), actual.getConfig());
    for (int y = 0; y < expected.getHeight(); y++) {
      for (int x = 0; x < expected.getWidth(); x++) {
        assertEquals(expected.getPixel(x, y), actual.getPixel(x, y));
      }
    }
  }

  private Bitmap createBitmapWithRedCircle(int width, int height) {
    int minEdge = Math.min(width, height);
    float radius = minEdge / 2f;

    Bitmap result = Bitmap.createBitmap(minEdge, minEdge, Bitmap.Config.ARGB_8888);
    result.setHasAlpha(true);
    Canvas canvas = new Canvas(result);
    Paint paint = new Paint();
    paint.setAntiAlias(true);
    paint.setColor(Color.RED);

    canvas.drawCircle(radius, radius, radius, paint);
    return result;
  }

  private Bitmap createSolidRedBitmap(int width, int height) {
    Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(result);
    Paint paint = new Paint();
    paint.setColor(Color.RED);
    Rect rect = new Rect(0, 0, width, height);
    canvas.drawRect(rect, paint);
    return result;
  }
}
