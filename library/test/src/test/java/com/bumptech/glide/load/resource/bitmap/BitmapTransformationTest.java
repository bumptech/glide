package com.bumptech.glide.load.resource.bitmap;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.tests.Util;
import java.security.MessageDigest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class BitmapTransformationTest {

  @Mock private BitmapPool bitmapPool;
  private Application context;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    context = ApplicationProvider.getApplicationContext();

    Glide.init(context, new GlideBuilder().setBitmapPool(bitmapPool));
  }

  @After
  public void tearDown() {
    Glide.tearDown();
  }

  @Test
  public void testReturnsGivenResourceWhenBitmapNotTransformed() {
    BitmapTransformation transformation =
        new BitmapTransformation() {
          @Override
          public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {}

          @Override
          protected Bitmap transform(
              @NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
            return toTransform;
          }
        };

    Resource<Bitmap> resource = mockResource(100, 100);
    assertEquals(resource, transformation.transform(context, resource, 1, 1));
  }

  @Test
  public void testReturnsNewResourceWhenBitmapTransformed() {
    final Bitmap transformed = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_4444);
    BitmapTransformation transformation =
        new BitmapTransformation() {
          @Override
          public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {}

          @Override
          protected Bitmap transform(
              @NonNull BitmapPool pool, @NonNull Bitmap bitmap, int outWidth, int outHeight) {
            return transformed;
          }
        };

    Resource<Bitmap> resource = mockResource(1, 2);
    assertNotSame(resource, transformation.transform(context, resource, 100, 100));
  }

  @Test
  public void testPassesGivenArgumentsToTransform() {
    final int expectedWidth = 13;
    final int expectedHeight = 148;
    final Resource<Bitmap> resource = mockResource(223, 4123);
    BitmapTransformation transformation =
        new BitmapTransformation() {
          @Override
          public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {}

          @Override
          protected Bitmap transform(
              @NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
            assertEquals(bitmapPool, pool);
            assertEquals(resource.get(), toTransform);
            assertEquals(expectedWidth, outWidth);
            assertEquals(expectedHeight, outHeight);
            return resource.get();
          }
        };

    transformation.transform(context, resource, expectedWidth, expectedHeight);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfGivenInvalidWidth() {
    BitmapTransformation transformation =
        new BitmapTransformation() {

          @Override
          public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {}

          @Override
          protected Bitmap transform(
              @NonNull BitmapPool bitmapPool,
              @NonNull Bitmap toTransform,
              int outWidth,
              int outHeight) {
            return null;
          }
        };
    transformation.transform(context, mockResource(1, 1), -1, 100);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfGivenInvalidHeight() {
    BitmapTransformation transformation =
        new BitmapTransformation() {

          @Override
          public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {}

          @Override
          protected Bitmap transform(
              @NonNull BitmapPool bitmapPool,
              @NonNull Bitmap toTransform,
              int outWidth,
              int outHeight) {
            return null;
          }
        };
    transformation.transform(context, mockResource(1, 1), 100, -1);
  }

  @Test
  public void testReturnsNullIfTransformReturnsNull() {
    BitmapTransformation transform =
        new BitmapTransformation() {

          @Override
          public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {}

          @Override
          protected Bitmap transform(
              @NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
            return null;
          }
        };

    Resource<Bitmap> resource = mockResource(100, 100);
    assertNull(transform.transform(context, resource, 100, 100));
  }

  @Test
  public void testCallsTransformWithGivenBitmapWidthIfWidthIsSizeOriginal() {
    SizeTrackingTransform transform = new SizeTrackingTransform();

    int expectedWidth = 200;
    Resource<Bitmap> resource = mockResource(expectedWidth, 300);
    transform.transform(context, resource, Target.SIZE_ORIGINAL, 500);

    assertEquals(expectedWidth, transform.givenWidth);
  }

  @Test
  public void testCallsTransformWithGivenBitmapHeightIfHeightIsSizeOriginal() {
    SizeTrackingTransform transform = new SizeTrackingTransform();

    int expectedHeight = 500;
    Resource<Bitmap> resource = mockResource(123, expectedHeight);
    transform.transform(context, resource, 444, expectedHeight);

    assertEquals(expectedHeight, transform.givenHeight);
  }

  private Resource<Bitmap> mockResource(int width, int height) {
    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Resource<Bitmap> resource = Util.mockResource();
    when(resource.get()).thenReturn(bitmap);
    return resource;
  }

  private static final class SizeTrackingTransform extends BitmapTransformation {
    int givenWidth;
    int givenHeight;

    @Override
    protected Bitmap transform(
        @NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
      givenWidth = outWidth;
      givenHeight = outHeight;
      return null;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {}
  }
}
