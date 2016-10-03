package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.tests.KeyAssertions;
import com.bumptech.glide.tests.Util;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowCanvas;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18, shadows = { CenterInsideTest.DrawNothingCanvas.class })
public class CenterInsideTest {

  @Mock Resource<Bitmap> resource;
  @Mock Transformation<Bitmap> transformation;
  private BitmapPool pool;
  private CenterInside centerInside;
  private int bitmapWidth;
  private int bitmapHeight;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    bitmapWidth = 100;
    bitmapHeight = 100;
    Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
    when(resource.get()).thenReturn(bitmap);

    pool = new BitmapPoolAdapter();

    centerInside = new CenterInside(pool);
  }

  @Test
  public void testReturnsGivenResourceIfMatchesSizeExactly() {
    Resource<Bitmap> result =
            centerInside.transform(resource, bitmapWidth, bitmapHeight);

    assertEquals(resource, result);
  }

  @Test
  public void testReturnsGivenResourceIfSmallerThanTarget() {
    Resource<Bitmap> result =
        centerInside.transform(resource, 150, 150);

    assertEquals(resource, result);
  }

  @Test
  public void testReturnsNewResourceIfLargerThanTarget() {
    Resource<Bitmap> result =
        centerInside.transform(resource, 50, 50);

    assertNotEquals(resource, result);
  }


  @Test
  public void testDoesNotRecycleGivenResourceIfMatchesSizeExactly() {
    centerInside.transform(resource, bitmapWidth, bitmapHeight);

    verify(resource, never()).recycle();
  }

  @Test
  public void testDoesNotRecycleGivenResource() {
    centerInside.transform(resource, 50, 50);

    verify(resource, never()).recycle();
  }

  @Test
  public void testEquals() throws NoSuchAlgorithmException {
    KeyAssertions.assertSame(centerInside, new CenterInside(pool));

    doAnswer(new Util.WriteDigest("other")).when(transformation)
        .updateDiskCacheKey(any(MessageDigest.class));
    KeyAssertions.assertDifferent(centerInside, transformation);
  }

  @Implements(Canvas.class)
  public static final class DrawNothingCanvas extends ShadowCanvas {

    @Implementation
    @Override
    public void drawBitmap(Bitmap bitmap, Matrix matrix, Paint paint) {
      // Do nothing.
    }
  }
}
