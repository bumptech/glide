package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Range;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Preconditions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowBitmap;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18, shadows = {
    TransformationUtilsTest.AlphaShadowBitmap.class })
public class TransformationUtilsTest {

  @Test
  public void testFitCenterWithWideBitmap() {
    final int maxSide = 500;

    Bitmap wide = Bitmap.createBitmap(2000, 100, Bitmap.Config.ARGB_8888);

    Bitmap transformed =
        TransformationUtils.fitCenter(wide, mock(BitmapPool.class), maxSide, maxSide);

    assertHasOriginalAspectRatio(wide, transformed);
    assertBitmapFitsExactlyWithinBounds(maxSide, transformed);
  }

  @Test
  public void testFitCenterWithSmallWideBitmap() {
    final int maxSide = 500;

    Bitmap smallWide = Bitmap.createBitmap(400, 40, Bitmap.Config.ARGB_8888);

    Bitmap transformed =
        TransformationUtils.fitCenter(smallWide, mock(BitmapPool.class), maxSide, maxSide);

    assertHasOriginalAspectRatio(smallWide, transformed);
    assertBitmapFitsExactlyWithinBounds(maxSide, transformed);
  }

  @Test
  public void testFitCenterWithTallBitmap() {
    final int maxSide = 500;

    Bitmap tall = Bitmap.createBitmap(65, 3000, Bitmap.Config.ARGB_8888);

    Bitmap transformed =
        TransformationUtils.fitCenter(tall, mock(BitmapPool.class), maxSide, maxSide);

    assertHasOriginalAspectRatio(tall, transformed);
    assertBitmapFitsExactlyWithinBounds(maxSide, transformed);
  }

  @Test
  public void testFitCenterWithSmallTallBitmap() {
    final int maxSide = 500;

    Bitmap smallTall = Bitmap.createBitmap(10, 400, Bitmap.Config.ARGB_8888);

    Bitmap transformed =
        TransformationUtils.fitCenter(smallTall, mock(BitmapPool.class), maxSide, maxSide);

    assertHasOriginalAspectRatio(smallTall, transformed);
    assertBitmapFitsExactlyWithinBounds(maxSide, transformed);
  }

  @Test
  public void testFitCenterWithSquareBitmap() {
    final int maxSide = 500;

    Bitmap square = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888);

    Bitmap transformed =
        TransformationUtils.fitCenter(square, mock(BitmapPool.class), maxSide, maxSide);

    assertHasOriginalAspectRatio(square, transformed);
    assertBitmapFitsExactlyWithinBounds(maxSide, transformed);
  }

  @Test
  public void testFitCenterWithTooSmallSquareBitmap() {
    final int maxSide = 500;

    Bitmap smallSquare = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

    Bitmap transformed =
        TransformationUtils.fitCenter(smallSquare, mock(BitmapPool.class), maxSide, maxSide);

    assertHasOriginalAspectRatio(smallSquare, transformed);
    assertBitmapFitsExactlyWithinBounds(maxSide, transformed);
  }

  // Test for Issue #195.
  @Test
  public void testFitCenterUsesFloorInsteadOfRoundingForOutputBitmapSize() {
    Bitmap toTransform = Bitmap.createBitmap(1230, 1640, Bitmap.Config.RGB_565);

    Bitmap transformed =
        TransformationUtils.fitCenter(toTransform, mock(BitmapPool.class), 1075, 1366);

    assertEquals(1024, transformed.getWidth());
    assertEquals(1366, transformed.getHeight());
  }

  @Test
  public void testFitCenterReturnsGivenBitmapIfGivenBitmapMatchesExactly() {
    Bitmap toFit = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_4444);
    Bitmap transformed =
        TransformationUtils.fitCenter(toFit, null, toFit.getWidth(), toFit.getHeight());
    assertTrue(toFit == transformed);
  }

  @Test
  public void testFitCenterReturnsGivenBitmapIfGivenBitmapWidthMatchesExactly() {
    Bitmap toFit = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_4444);
    Bitmap transformed =
        TransformationUtils.fitCenter(toFit, null, toFit.getWidth(), toFit.getHeight() * 2);
    assertTrue(toFit == transformed);
  }

  @Test
  public void testFitCenterReturnsGivenBitmapIfGivenBitmapHeightMatchesExactly() {
    Bitmap toFit = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_4444);
    Bitmap transformed =
        TransformationUtils.fitCenter(toFit, null, toFit.getWidth() * 2, toFit.getHeight());
    assertTrue(toFit == transformed);
  }

  @Test
  public void testCenterCropReturnsNullIfGivenBitmapIsNull() {
    Bitmap transformed =
        TransformationUtils.centerCrop(null /*recycled*/, null /*toCrop*/, 100, 100);
    assertNull(transformed);
  }

  @Test
  public void testCenterCropReturnsGivenBitmapIfGivenBitmapExactlyMatchesGivenDimensions() {
    Bitmap toCrop = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888);
    Bitmap transformed = TransformationUtils
        .centerCrop(null /*recycled*/, toCrop, toCrop.getWidth(), toCrop.getHeight());

    // Robolectric incorrectly implements equals() for Bitmaps, we want the original object not
    // just an equivalent.
    assertTrue(toCrop == transformed);
  }

  @Test
  public void testFitCenterHandlesBitmapsWithNullConfigs() {
    Bitmap toFit = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
    Shadows.shadowOf(toFit).setConfig(null);
    Bitmap transformed = TransformationUtils.fitCenter(toFit, mock(BitmapPool.class), 50, 50);
    assertEquals(Bitmap.Config.ARGB_8888, transformed.getConfig());
  }

  @Test
  public void testCenterCropSetsOutBitmapToHaveAlphaIfInBitmapHasAlphaAndOutBitmapIsReused() {
    Bitmap toTransform = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

    Bitmap toReuse = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);

    toReuse.setHasAlpha(false);
    toTransform.setHasAlpha(true);

    Bitmap result = TransformationUtils
        .centerCrop(toReuse, toTransform, toReuse.getWidth(), toReuse.getHeight());

    assertEquals(toReuse, result);
    assertTrue(result.hasAlpha());
  }

  @Test
  public void
  testCenterCropSetsOutBitmapToNotHaveAlphaIfInBitmapDoesNotHaveAlphaAndOutBitmapIsReused() {
    Bitmap toTransform = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

    Bitmap toReuse = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);

    toReuse.setHasAlpha(true);
    toTransform.setHasAlpha(false);

    Bitmap result = TransformationUtils
        .centerCrop(toReuse, toTransform, toReuse.getWidth(), toReuse.getHeight());

    assertEquals(toReuse, result);
    assertFalse(result.hasAlpha());
  }

  @Test
  public void testCenterCropSetsOutBitmapToHaveAlphaIfInBitmapHasAlpha() {
    Bitmap toTransform = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

    toTransform.setHasAlpha(true);

    Bitmap result = TransformationUtils
        .centerCrop(null, toTransform, toTransform.getWidth() / 2, toTransform.getHeight() / 2);

    assertTrue(result.hasAlpha());
  }

  @Test
  public void testCenterCropHandlesBitmapsWithNullConfigs() {
    Bitmap toTransform = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
    Shadows.shadowOf(toTransform).setConfig(null);

    Bitmap transformed = TransformationUtils.centerCrop(null /*recycled*/, toTransform, 50, 50);

    assertEquals(Bitmap.Config.ARGB_8888, transformed.getConfig());
  }

  @Test
  public void testCenterCropSetsOutBitmapToNotHaveAlphaIfInBitmapDoesNotHaveAlpha() {
    Bitmap toTransform = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

    toTransform.setHasAlpha(false);

    Bitmap result = TransformationUtils
        .centerCrop(null, toTransform, toTransform.getWidth() / 2, toTransform.getHeight() / 2);

    assertFalse(result.hasAlpha());
  }

  @Test
  public void testFitCenterSetsOutBitmapToHaveAlphaIfInBitmapHasAlphaAndOutBitmapIsReused() {
    Bitmap toTransform = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

    Bitmap toReuse = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
    BitmapPool bitmapPool = mock(BitmapPool.class);
    when(bitmapPool.get(eq(toReuse.getWidth()), eq(toReuse.getHeight()), eq(toReuse.getConfig())))
        .thenReturn(toReuse);

    toReuse.setHasAlpha(false);
    toTransform.setHasAlpha(true);

    Bitmap result = TransformationUtils
        .fitCenter(toTransform, bitmapPool, toReuse.getWidth(), toReuse.getHeight());

    assertEquals(toReuse, result);
    assertTrue(result.hasAlpha());
  }

  @Test
  public void
  testFitCenterSetsOutBitmapToNotHaveAlphaIfInBitmapDoesNotHaveAlphaAndOutBitmapIsReused() {
    Bitmap toTransform = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

    Bitmap toReuse = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
    BitmapPool bitmapPool = mock(BitmapPool.class);
    when(bitmapPool.get(eq(toReuse.getWidth()), eq(toReuse.getHeight()), eq(toReuse.getConfig())))
        .thenReturn(toReuse);

    toReuse.setHasAlpha(true);
    toTransform.setHasAlpha(false);

    Bitmap result = TransformationUtils
        .fitCenter(toTransform, bitmapPool, toReuse.getWidth(), toReuse.getHeight());

    assertEquals(toReuse, result);
    assertFalse(result.hasAlpha());
  }

  @Test
  public void testFitCenterSetsOutBitmapToHaveAlphaIfInBitmapHasAlpha() {
    Bitmap toTransform = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    BitmapPool pool = mock(BitmapPool.class);

    toTransform.setHasAlpha(true);

    Bitmap result = TransformationUtils
        .fitCenter(toTransform, pool, toTransform.getWidth() / 2, toTransform.getHeight() / 2);

    assertTrue(result.hasAlpha());
  }

  @Test
  public void testFitCenterSetsOutBitmapToNotHaveAlphaIfInBitmapDoesNotHaveAlpha() {
    Bitmap toTransform = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    BitmapPool pool = mock(BitmapPool.class);

    toTransform.setHasAlpha(false);

    Bitmap result = TransformationUtils
        .fitCenter(toTransform, pool, toTransform.getWidth() / 2, toTransform.getHeight() / 2);

    assertFalse(result.hasAlpha());
  }

  private static void assertHasOriginalAspectRatio(Bitmap original, Bitmap transformed) {
    double originalAspectRatio = (double) original.getWidth() / (double) original.getHeight();
    double transformedAspectRatio =
        (double) transformed.getWidth() / (double) transformed.getHeight();

    assertThat(transformedAspectRatio)
        .isIn(Range.open(originalAspectRatio - 0.05f, originalAspectRatio + 0.05f));
  }

  private static void assertBitmapFitsExactlyWithinBounds(int maxSide, Bitmap bitmap) {
    final int width = bitmap.getWidth();
    final int height = bitmap.getHeight();

    assertThat(width).isIn(Range.atMost(maxSide));
    assertThat(height).isIn(Range.atMost(maxSide));

    assertTrue("one side must match maxSide", width == maxSide || height == maxSide);
  }

  @Test
  public void testGetExifOrientationDegrees() {
    assertEquals(0,
        TransformationUtils.getExifOrientationDegrees(ExifInterface.ORIENTATION_NORMAL));
    assertEquals(90,
        TransformationUtils.getExifOrientationDegrees(ExifInterface.ORIENTATION_TRANSPOSE));
    assertEquals(90,
        TransformationUtils.getExifOrientationDegrees(ExifInterface.ORIENTATION_ROTATE_90));
    assertEquals(180,
        TransformationUtils.getExifOrientationDegrees(ExifInterface.ORIENTATION_ROTATE_180));
    assertEquals(180,
        TransformationUtils.getExifOrientationDegrees(ExifInterface.ORIENTATION_FLIP_VERTICAL));
    assertEquals(270,
        TransformationUtils.getExifOrientationDegrees(ExifInterface.ORIENTATION_TRANSVERSE));
    assertEquals(270,
        TransformationUtils.getExifOrientationDegrees(ExifInterface.ORIENTATION_ROTATE_270));
  }

  @Test
  public void testRotateImage() {
    Bitmap toRotate = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);

    Bitmap zero = TransformationUtils.rotateImage(toRotate, 0);
    assertTrue(toRotate == zero);

    Bitmap ninety = TransformationUtils.rotateImage(toRotate, 90);
    assertTrue(Shadows.shadowOf(ninety).getDescription().contains("rotate=90.0"));
    assertEquals(toRotate.getWidth(), toRotate.getHeight());
  }

  @Test
  public void testRotateImageExifReturnsGivenBitmapIfRotationIsNormal() {
    BitmapPool bitmapPool = mock(BitmapPool.class);
    Bitmap toRotate = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_4444);
    // Use assertTrue because Robolectric incorrectly implements equality for Bitmaps. We want
    // not just an
    // identical Bitmap, but our original Bitmap object back.
    Bitmap rotated =
        TransformationUtils.rotateImageExif(toRotate, bitmapPool, ExifInterface.ORIENTATION_NORMAL);
    assertTrue(toRotate == rotated);
  }

  @Test
  public void testRotateImageExifReturnsGivenBitmapIfRotationIsUndefined() {
    BitmapPool bitmapPool = mock(BitmapPool.class);
    Bitmap toRotate = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
    // Use assertTrue because Robolectric incorrectly implements equality for Bitmaps. We want
    // not just an
    // identical Bitmap, but our original Bitmap object back.
    Bitmap rotated = TransformationUtils
        .rotateImageExif(toRotate, bitmapPool, ExifInterface.ORIENTATION_UNDEFINED);
    assertTrue(toRotate == rotated);
  }

  @Test
  public void testRotateImageExifReturnsGivenBitmapIfOrientationIsInvalid() {
    BitmapPool bitmapPool = mock(BitmapPool.class);
    Bitmap toRotate = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888);
    // Use assertTrue because Robolectric incorrectly implements equality for Bitmaps. We want
    // not just an
    // identical Bitmap, but our original Bitmap object back.
    Bitmap rotated = TransformationUtils.rotateImageExif(toRotate, bitmapPool, -1);
    assertTrue(toRotate == rotated);
  }

  @Test
  public void testRotateImageExifHandlesEmptyBitmapPool() {
    Bitmap toRotate = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_4444);
    BitmapPool bitmapPool = mock(BitmapPool.class);
    assertNotNull(TransformationUtils
        .rotateImageExif(toRotate, bitmapPool, ExifInterface.ORIENTATION_ROTATE_90));
  }

  @Test
  public void testRotateImageExifHandlesBitmapsWithNullConfigs() {
    Bitmap toRotate = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
    Shadows.shadowOf(toRotate).setConfig(null);
    BitmapPool bitmapPool = mock(BitmapPool.class);
    Bitmap rotated = TransformationUtils
        .rotateImageExif(toRotate, bitmapPool, ExifInterface.ORIENTATION_ROTATE_180);
    assertEquals(Bitmap.Config.ARGB_8888, rotated.getConfig());
  }

  @Test
  public void testInitializeMatrixSetsScaleIfFlipHorizontal() {
    Matrix matrix = mock(Matrix.class);
    TransformationUtils
        .initializeMatrixForRotation(ExifInterface.ORIENTATION_FLIP_HORIZONTAL, matrix);
    verify(matrix).setScale(-1, 1);
  }

  @Test
  public void testInitializeMatrixSetsScaleAndRotateIfFlipVertical() {
    Matrix matrix = mock(Matrix.class);
    TransformationUtils
        .initializeMatrixForRotation(ExifInterface.ORIENTATION_FLIP_VERTICAL, matrix);
    verify(matrix).setRotate(180);
    verify(matrix).postScale(-1, 1);
  }

  @Test
  public void testInitializeMatrixSetsScaleAndRotateIfTranspose() {
    Matrix matrix = mock(Matrix.class);
    TransformationUtils.initializeMatrixForRotation(ExifInterface.ORIENTATION_TRANSPOSE, matrix);
    verify(matrix).setRotate(90);
    verify(matrix).postScale(-1, 1);
  }

  @Test
  public void testInitializeMatrixSetsScaleAndRotateIfTransverse() {
    Matrix matrix = mock(Matrix.class);
    TransformationUtils.initializeMatrixForRotation(ExifInterface.ORIENTATION_TRANSVERSE, matrix);
    verify(matrix).setRotate(-90);
    verify(matrix).postScale(-1, 1);
  }

  @Test
  public void testInitializeMatrixSetsRotateOnRotation() {
    Matrix matrix = mock(Matrix.class);
    TransformationUtils.initializeMatrixForRotation(ExifInterface.ORIENTATION_ROTATE_90, matrix);
    verify(matrix).setRotate(90);
    TransformationUtils.initializeMatrixForRotation(ExifInterface.ORIENTATION_ROTATE_180, matrix);
    verify(matrix).setRotate(180);
    TransformationUtils.initializeMatrixForRotation(ExifInterface.ORIENTATION_ROTATE_270, matrix);
    verify(matrix).setRotate(-90);
  }

  @Implements(Bitmap.class)
  public static class AlphaShadowBitmap extends ShadowBitmap {

    private boolean hasAlpha;

    @Implementation
    public static Bitmap createBitmap(int width, int height, Bitmap.Config config) {
      // Robolectric doesn't match the framework behavior with null configs, so we have to do so
      // here.
      Preconditions.checkNotNull("Config must not be null");
      return ShadowBitmap.createBitmap(width, height, config);
    }

    @Implementation
    public void setHasAlpha(boolean hasAlpha) {
      this.hasAlpha = hasAlpha;
    }

    @Implementation
    public boolean hasAlpha() {
      return hasAlpha;
    }
  }
}
