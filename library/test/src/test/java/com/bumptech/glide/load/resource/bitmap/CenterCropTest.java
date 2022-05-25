package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.graphics.Bitmap;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.tests.KeyTester;
import com.bumptech.glide.tests.Util;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class CenterCropTest {
  @Rule public final KeyTester keyTester = new KeyTester();
  @Mock private Resource<Bitmap> resource;
  @Mock private BitmapPool pool;
  @Mock private Transformation<Bitmap> transformation;

  private CenterCrop centerCrop;
  private int bitmapWidth;
  private int bitmapHeight;
  private Bitmap bitmap;
  private Application context;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    bitmapWidth = 100;
    bitmapHeight = 100;
    bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
    when(resource.get()).thenReturn(bitmap);

    when(pool.get(anyInt(), anyInt(), any(Bitmap.Config.class)))
        .thenAnswer(new Util.CreateBitmap());
    context = ApplicationProvider.getApplicationContext();
    Glide.init(context, new GlideBuilder().setBitmapPool(pool));

    centerCrop = new CenterCrop();
  }

  @After
  public void tearDown() {
    Glide.tearDown();
  }

  @Test
  public void testDoesNotPutNullBitmapAcquiredFromPool() {
    reset(pool);
    when(pool.get(anyInt(), anyInt(), any(Bitmap.Config.class))).thenReturn(null);

    centerCrop.transform(context, resource, 100, 100);

    verify(pool, never()).put(any(Bitmap.class));
  }

  @Test
  public void testReturnsGivenResourceIfMatchesSizeExactly() {
    Resource<Bitmap> result = centerCrop.transform(context, resource, bitmapWidth, bitmapHeight);

    assertEquals(resource, result);
  }

  @Test
  public void testDoesNotRecycleGivenResourceIfMatchesSizeExactly() {
    centerCrop.transform(context, resource, bitmapWidth, bitmapHeight);

    verify(resource, never()).recycle();
  }

  @Test
  public void testDoesNotRecycleGivenResource() {
    centerCrop.transform(context, resource, 50, 50);

    verify(resource, never()).recycle();
  }

  @Test
  @Config(sdk = 19)
  public void testAsksBitmapPoolForArgb8888IfInConfigIsNull() {
    bitmap.setConfig(null);

    centerCrop.transform(context, resource, 10, 10);

    verify(pool).get(anyInt(), anyInt(), eq(Bitmap.Config.ARGB_8888));
    verify(pool, never()).get(anyInt(), anyInt(), (Bitmap.Config) isNull());
  }

  @Test
  public void testReturnsBitmapWithExactlyGivenDimensionsIfBitmapIsLargerThanTarget() {
    int expectedWidth = 75;
    int expectedHeight = 74;

    for (int[] dimens :
        new int[][] {new int[] {800, 200}, new int[] {450, 100}, new int[] {78, 78}}) {
      Bitmap toTransform = Bitmap.createBitmap(dimens[0], dimens[1], Bitmap.Config.ARGB_4444);
      when(resource.get()).thenReturn(toTransform);

      Resource<Bitmap> result =
          centerCrop.transform(context, resource, expectedWidth, expectedHeight);
      Bitmap transformed = result.get();
      assertEquals(expectedWidth, transformed.getWidth());
      assertEquals(expectedHeight, transformed.getHeight());
    }
  }

  @Test
  public void testReturnsBitmapWithExactlyGivenDimensionsIfBitmapIsSmallerThanTarget() {
    int expectedWidth = 100;
    int expectedHeight = 100;

    for (int[] dimens : new int[][] {new int[] {50, 90}, new int[] {150, 2}, new int[] {78, 78}}) {
      Bitmap toTransform = Bitmap.createBitmap(dimens[0], dimens[1], Bitmap.Config.ARGB_4444);
      when(resource.get()).thenReturn(toTransform);

      Resource<Bitmap> result =
          centerCrop.transform(context, resource, expectedWidth, expectedHeight);
      Bitmap transformed = result.get();
      assertEquals(expectedWidth, transformed.getWidth());
      assertEquals(expectedHeight, transformed.getHeight());
    }
  }

  @Test
  public void testEquals() throws NoSuchAlgorithmException {
    doAnswer(new Util.WriteDigest("other"))
        .when(transformation)
        .updateDiskCacheKey(any(MessageDigest.class));
    keyTester
        .addEquivalenceGroup(new CenterCrop(), new CenterCrop())
        .addEquivalenceGroup(transformation)
        .addRegressionTest(
            new CenterCrop(), "68bd5819c42b37efbe7124bb851443a6388ee3e2e9034213da6eaa15381d3457")
        .test();
  }
}
