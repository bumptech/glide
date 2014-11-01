package com.bumptech.glide.util;

import android.graphics.Bitmap;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;
import org.hamcrest.core.CombinableMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowBitmap;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18, shadows = { TransformationUtilsTest.AlphaShadowBitmap.class })
public class TransformationUtilsTest {

    @Test
    public void testFitCenterWithWideBitmap() {
        final int maxSide = 500;

        Bitmap wide = Bitmap.createBitmap(2000, 100, Bitmap.Config.ARGB_8888);

        Bitmap transformed = TransformationUtils.fitCenter(wide, mock(BitmapPool.class), maxSide, maxSide);

        assertHasOriginalAspectRatio(wide, transformed);
        assertBitmapFitsExactlyWithinBounds(maxSide, transformed);
    }

    @Test
    public void testFitCenterWithSmallWideBitmap() {
        final int maxSide = 500;

        Bitmap smallWide = Bitmap.createBitmap(400, 40, Bitmap.Config.ARGB_8888);

        Bitmap transformed = TransformationUtils.fitCenter(smallWide, mock(BitmapPool.class), maxSide, maxSide);

        assertHasOriginalAspectRatio(smallWide, transformed);
        assertBitmapFitsExactlyWithinBounds(maxSide, transformed);
    }

    @Test
    public void testFitCenterWithTallBitmap() {
        final int maxSide = 500;

        Bitmap tall = Bitmap.createBitmap(65, 3000, Bitmap.Config.ARGB_8888);

        Bitmap transformed = TransformationUtils.fitCenter(tall, mock(BitmapPool.class), maxSide, maxSide);

        assertHasOriginalAspectRatio(tall, transformed);
        assertBitmapFitsExactlyWithinBounds(maxSide, transformed);
    }

    @Test
    public void testFitCenterWithSmallTallBitmap() {
        final int maxSide = 500;

        Bitmap smallTall = Bitmap.createBitmap(10, 400, Bitmap.Config.ARGB_8888);

        Bitmap transformed = TransformationUtils.fitCenter(smallTall, mock(BitmapPool.class), maxSide, maxSide);

        assertHasOriginalAspectRatio(smallTall, transformed);
        assertBitmapFitsExactlyWithinBounds(maxSide, transformed);
    }

    @Test
    public void testFitCenterWithSquareBitmap() {
        final int maxSide = 500;

        Bitmap square = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888);

        Bitmap transformed = TransformationUtils.fitCenter(square, mock(BitmapPool.class), maxSide, maxSide);

        assertHasOriginalAspectRatio(square, transformed);
        assertBitmapFitsExactlyWithinBounds(maxSide, transformed);
    }

    @Test
    public void testFitCenterWithTooSmallSquareBitmap() {
        final int maxSide = 500;

        Bitmap smallSquare = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

        Bitmap transformed = TransformationUtils.fitCenter(smallSquare, mock(BitmapPool.class), maxSide, maxSide);

        assertHasOriginalAspectRatio(smallSquare, transformed);
        assertBitmapFitsExactlyWithinBounds(maxSide, transformed);
    }

    // Test for Issue #195.
    @Test
    public void testFitCenterUsesFloorInsteadofRoundingForOutputBitmapSize() {
        Bitmap toTransform = Bitmap.createBitmap(1230, 1640, Bitmap.Config.RGB_565);

        Bitmap transformed = TransformationUtils.fitCenter(toTransform, mock(BitmapPool.class), 1075, 1366);

        assertEquals(1024, transformed.getWidth());
        assertEquals(1366, transformed.getHeight());
    }

    @Test
    public void testCenterCropSetsOutBitmapToHaveAlphaIfInBitmapHasAlphaAndOutBitmapIsReused() {
        Bitmap toTransform = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

        Bitmap toReuse = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);

        toReuse.setHasAlpha(false);
        toTransform.setHasAlpha(true);

        Bitmap result = TransformationUtils.centerCrop(toReuse, toTransform, toReuse.getWidth(), toReuse.getHeight());

        assertEquals(toReuse, result);
        assertTrue(result.hasAlpha());
    }

    @Test
    public void testCenterCropSetsOutBitmapToNotHaveAlphaIfInBitmapDoesNotHaveAlphaAndOutBitmapIsReused() {
        Bitmap toTransform = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

        Bitmap toReuse = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);

        toReuse.setHasAlpha(true);
        toTransform.setHasAlpha(false);

        Bitmap result = TransformationUtils.centerCrop(toReuse, toTransform, toReuse.getWidth(), toReuse.getHeight());

        assertEquals(toReuse, result);
        assertFalse(result.hasAlpha());
    }

    @Test
    public void testCenterCropSetsOutBitmapToHaveAlphaIfInBitmapHasAlpha() {
        Bitmap toTransform = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        BitmapPool pool = mock(BitmapPool.class);

        toTransform.setHasAlpha(true);

        Bitmap result = TransformationUtils.centerCrop(null, toTransform, toTransform.getWidth() / 2,
                toTransform.getHeight() / 2);

        assertTrue(result.hasAlpha());
    }

    @Test
    public void testSetsOutBitmapToNotHaveAlphaIfInBitmapDoesNotHaveAlpha() {
        Bitmap toTransform = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

        toTransform.setHasAlpha(false);

        Bitmap result = TransformationUtils.centerCrop(null, toTransform, toTransform.getWidth() / 2,
                toTransform.getHeight() / 2);

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

        Bitmap result = TransformationUtils.fitCenter(toTransform, bitmapPool, toReuse.getWidth(), toReuse.getHeight());

        assertEquals(toReuse, result);
        assertTrue(result.hasAlpha());
    }

    @Test
    public void testFitCenterSetsOutBitmapToNotHaveAlphaIfInBitmapDoesNotHaveAlphaAndOutBitmapIsReused() {
        Bitmap toTransform = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

        Bitmap toReuse = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        BitmapPool bitmapPool = mock(BitmapPool.class);
        when(bitmapPool.get(eq(toReuse.getWidth()), eq(toReuse.getHeight()), eq(toReuse.getConfig())))
                .thenReturn(toReuse);

        toReuse.setHasAlpha(true);
        toTransform.setHasAlpha(false);

        Bitmap result = TransformationUtils.fitCenter(toTransform, bitmapPool, toReuse.getWidth(), toReuse.getHeight());

        assertEquals(toReuse, result);
        assertFalse(result.hasAlpha());
    }

    @Test
    public void testFitCenterSetsOutBitmapToHaveAlphaIfInBitmapHasAlpha() {
        Bitmap toTransform = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        BitmapPool pool = mock(BitmapPool.class);

        toTransform.setHasAlpha(true);

        Bitmap result = TransformationUtils.fitCenter(toTransform, pool, toTransform.getWidth() / 2,
                toTransform.getHeight() / 2);

        assertTrue(result.hasAlpha());
    }

    @Test
    public void testFitCenterSetsOutBitmapToNotHaveAlphaIfInBitmapDoesNotHaveAlpha() {
        Bitmap toTransform = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        BitmapPool pool = mock(BitmapPool.class);

        toTransform.setHasAlpha(false);

        Bitmap result = TransformationUtils.fitCenter(toTransform, pool, toTransform.getWidth() / 2,
                toTransform.getHeight() / 2);

        assertFalse(result.hasAlpha());
    }

    private static void assertHasOriginalAspectRatio(Bitmap original, Bitmap transformed) {
        double originalAspectRatio = (double) original.getWidth() / (double) original.getHeight();
        double transformedAspectRatio = (double) transformed.getWidth() / (double) transformed.getHeight();

        assertThat("nearly identical aspect ratios", transformedAspectRatio, closeTo(originalAspectRatio, 0.05));
    }

    private static void assertBitmapFitsExactlyWithinBounds(int maxSide, Bitmap bitmap) {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();

        assertThat("width", width, lessThanOrEqualTo(maxSide));
        assertThat("height", height, lessThanOrEqualTo(maxSide));

        // See https://code.google.com/p/hamcrest/issues/detail?id=82.
        CombinableMatcher.CombinableEitherMatcher<Integer> eitherMatcher = either(equalTo(width));
        assertThat("one side must match maxSide", maxSide, eitherMatcher.or(equalTo(height)));
    }

    @Implements(Bitmap.class)
    public static class AlphaShadowBitmap extends ShadowBitmap {

        private boolean hasAlpha;

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
