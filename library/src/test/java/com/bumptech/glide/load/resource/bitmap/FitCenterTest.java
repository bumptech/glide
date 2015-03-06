package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.tests.KeyAssertions;
import com.bumptech.glide.tests.Util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class FitCenterTest {

  @Mock BitmapPool pool;
  @Mock Resource<Bitmap> resource;
  private FitCenter fitCenter;
  private int bitmapWidth;
  private int bitmapHeight;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    bitmapWidth = 100;
    bitmapHeight = 100;
    Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
    when(resource.get()).thenReturn(bitmap);

    fitCenter = new FitCenter(pool);
  }

  @Test
  public void testDoesNotPutNullBitmapAcquiredFromPool() {
    when(pool.get(anyInt(), anyInt(), any(Bitmap.Config.class))).thenReturn(null);

    fitCenter.transform(resource, 100, 100);

    verify(pool, never()).put(any(Bitmap.class));
  }

  @Test
  public void testReturnsGivenResourceIfMatchesSizeExactly() {
    Resource<Bitmap> result =
        fitCenter.transform(resource, bitmapWidth, bitmapHeight);

    assertEquals(resource, result);
  }

  @Test
  public void testDoesNotRecycleGivenResourceIfMatchesSizeExactly() {
    fitCenter.transform(resource, bitmapWidth, bitmapHeight);

    verify(resource, never()).recycle();
  }

  @Test
  public void testDoesNotRecycleGivenResource() {
    fitCenter.transform(resource, 50, 50);

    verify(resource, never()).recycle();
  }

  @Test
  public void testEquals() throws NoSuchAlgorithmException {
    KeyAssertions.assertSame(fitCenter, new FitCenter(pool));

    Transformation<Bitmap> other = mock(Transformation.class);
    doAnswer(new Util.WriteDigest("other")).when(other)
        .updateDiskCacheKey(any(MessageDigest.class));
    KeyAssertions.assertDifferent(fitCenter, other);
  }
}
