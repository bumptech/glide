package com.bumptech.glide.load.resource.bitmap;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
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
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
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
@Config(sdk = ROBOLECTRIC_SDK)
public class CenterInsideTest {
  @Rule public final KeyTester keyTester = new KeyTester();

  @Mock private Resource<Bitmap> resource;
  @Mock private Transformation<Bitmap> transformation;
  private CenterInside centerInside;
  private int bitmapWidth;
  private int bitmapHeight;
  private Application context;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    bitmapWidth = 100;
    bitmapHeight = 100;
    Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
    when(resource.get()).thenReturn(bitmap);

    context = ApplicationProvider.getApplicationContext();
    BitmapPool pool = new BitmapPoolAdapter();
    Glide.init(context, new GlideBuilder().setBitmapPool(pool));

    centerInside = new CenterInside();
  }

  @After
  public void tearDown() {
    Glide.tearDown();
  }

  @Test
  public void testReturnsGivenResourceIfMatchesSizeExactly() {
    Resource<Bitmap> result = centerInside.transform(context, resource, bitmapWidth, bitmapHeight);

    assertEquals(resource, result);
  }

  @Test
  public void testReturnsGivenResourceIfSmallerThanTarget() {
    Resource<Bitmap> result = centerInside.transform(context, resource, 150, 150);

    assertEquals(resource, result);
  }

  @Test
  public void testReturnsNewResourceIfLargerThanTarget() {
    Resource<Bitmap> result = centerInside.transform(context, resource, 50, 50);

    assertNotEquals(resource, result);
  }

  @Test
  public void testDoesNotRecycleGivenResourceIfMatchesSizeExactly() {
    centerInside.transform(context, resource, bitmapWidth, bitmapHeight);

    verify(resource, never()).recycle();
  }

  @Test
  public void testDoesNotRecycleGivenResource() {
    centerInside.transform(context, resource, 50, 50);

    verify(resource, never()).recycle();
  }

  @Test
  public void testEquals() throws NoSuchAlgorithmException {
    doAnswer(new Util.WriteDigest("other"))
        .when(transformation)
        .updateDiskCacheKey(any(MessageDigest.class));

    keyTester
        .addEquivalenceGroup(new CenterInside(), new CenterInside(), centerInside)
        .addEquivalenceGroup(transformation)
        .addRegressionTest(
            new CenterInside(), "acf83850a2e8e9e809c8bfb999e2aede9e932cb897a15367fac9856b96f3ba33")
        .test();
  }
}
