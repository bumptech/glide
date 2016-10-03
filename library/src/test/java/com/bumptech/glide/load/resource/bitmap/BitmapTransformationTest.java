package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.tests.Util;
import java.security.MessageDigest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class BitmapTransformationTest {

  @Mock
  private BitmapPool bitmapPool;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testReturnsGivenResourceWhenBitmapNotTransformed() {
    BitmapTransformation transformation = new BitmapTransformation(bitmapPool) {
      @Override
      public void updateDiskCacheKey(MessageDigest messageDigest) { }

      @Override
      protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform,
          int outWidth, int outHeight) {
        return toTransform;
      }
    };

    Resource<Bitmap> resource = mockResource(100, 100);
    assertEquals(resource, transformation.transform(resource, 1, 1));
  }

  @Test
  public void testReturnsNewResourceWhenBitmapTransformed() {
    final Bitmap transformed = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_4444);
    BitmapTransformation transformation = new BitmapTransformation(bitmapPool) {
      @Override
      public void updateDiskCacheKey(MessageDigest messageDigest) { }

      @Override
      protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap bitmap, int outWidth,
          int outHeight) {
        return transformed;
      }
    };

    Resource<Bitmap> resource = mockResource(1, 2);
    assertNotSame(resource, transformation.transform(resource, 100, 100));
  }

  @Test
  public void testPassesGivenArgumentsToTransform() {
    final int expectedWidth = 13;
    final int expectedHeight = 148;
    final Resource<Bitmap> resource = mockResource(223, 4123);
    BitmapTransformation transformation = new BitmapTransformation(bitmapPool) {
      @Override
      public void updateDiskCacheKey(MessageDigest messageDigest) { }

      @Override
      protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform,
          int outWidth, int outHeight) {
        assertEquals(bitmapPool, pool);
        assertEquals(resource.get(), toTransform);
        assertEquals(expectedWidth, outWidth);
        assertEquals(expectedHeight, outHeight);
        return resource.get();
      }
    };

    transformation.transform(resource, expectedWidth, expectedHeight);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfGivenInvalidWidth() {
    BitmapTransformation transformation = new BitmapTransformation(bitmapPool) {

      @Override
      public void updateDiskCacheKey(MessageDigest messageDigest) { }

      @Override
      protected Bitmap transform(@NonNull BitmapPool bitmapPool, @NonNull Bitmap toTransform,
          int outWidth, int outHeight) {
        return null;
      }
    };
    transformation.transform(mockResource(1, 1), -1, 100);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfGivenInvalidHeight() {
    BitmapTransformation transformation = new BitmapTransformation(bitmapPool) {

      @Override
      public void updateDiskCacheKey(MessageDigest messageDigest) { }

      @Override
      protected Bitmap transform(@NonNull BitmapPool bitmapPool, @NonNull Bitmap toTransform,
          int outWidth, int outHeight) {
        return null;
      }

    };
    transformation.transform(mockResource(1, 1), 100, -1);
  }

  @Test
  public void testReturnsNullIfTransformReturnsNull() {
    BitmapTransformation transform = new BitmapTransformation(bitmapPool) {

      @Override
      public void updateDiskCacheKey(MessageDigest messageDigest) {  }

      @Override
      protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform,
          int outWidth, int outHeight) {
        return null;
      }
    };

    Resource<Bitmap> resource = mockResource(100, 100);
    assertNull(transform.transform(resource, 100, 100));
  }

  @Test
  public void testCallsTransformWithGivenBitmapWidthIfWidthIsSizeOriginal() {
    SizeTrackingTransform transform = new SizeTrackingTransform();

    int expectedWidth = 200;
    Resource<Bitmap> resource = mockResource(expectedWidth, 300);
    transform.transform(resource, Target.SIZE_ORIGINAL, 500);

    assertEquals(expectedWidth, transform.givenWidth);
  }

  @Test
  public void testCallsTransformWithGivenBitmapHeightIfHeightIsSizeOriginal() {
    SizeTrackingTransform transform = new SizeTrackingTransform();

    int expectedHeight = 500;
    Resource<Bitmap> resource = mockResource(123, expectedHeight);
    transform.transform(resource, 444, expectedHeight);

    assertEquals(expectedHeight, transform.givenHeight);
  }

  private Resource<Bitmap> mockResource(int width, int height) {
    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Resource<Bitmap> resource = Util.mockResource();
    when(resource.get()).thenReturn(bitmap);
    return resource;
  }

  private class SizeTrackingTransform extends BitmapTransformation {
    int givenWidth;
    int givenHeight;

    public SizeTrackingTransform() {
      super(bitmapPool);
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth,
        int outHeight) {
      givenWidth = outWidth;
      givenHeight = outHeight;
      return null;
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) { }
  }
}
