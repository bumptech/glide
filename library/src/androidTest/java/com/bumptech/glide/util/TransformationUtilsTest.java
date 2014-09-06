package com.bumptech.glide.util;

import android.graphics.Bitmap;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;
import org.hamcrest.core.CombinableMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
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
}
