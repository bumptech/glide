package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;
import android.test.AndroidTestCase;

public class SizeStrategyTest extends AndroidTestCase {
    private SizeStrategy strategy;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        strategy = new SizeStrategy();
    }

    public void testIGetNullIfNoMatchingBitmapExists() {
        assertNull(strategy.get(100, 100, Bitmap.Config.ARGB_8888));
    }

    public void testICanAddAndGetABitmapOfTheSameSizeAndDimensions() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        strategy.put(bitmap);
        assertEquals(bitmap, strategy.get(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888));
    }

    public void testICanAddAndGetABitmapOfDifferentConfigsButSameSize() {
        Bitmap original = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        strategy.put(original);
        assertEquals(original, strategy.get(800, 400, Bitmap.Config.RGB_565));
    }

    public void testICanAddAndGetABitmapOfDifferentDimensionsButSameSize() {
        Bitmap original = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        strategy.put(original);
        assertEquals(original, strategy.get(200, 800, Bitmap.Config.ARGB_8888));
    }

    public void testICanGetABitmapUpToFourTimesLarger() {
        Bitmap original = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        strategy.put(original);
        assertEquals(original, strategy.get(200, 200, Bitmap.Config.ARGB_8888));
    }

    public void testICantGetABitmapMoreThanFourTimesLarger() {
        Bitmap original = Bitmap.createBitmap(401, 401, Bitmap.Config.ARGB_8888);
        strategy.put(original);
        assertNull(strategy.get(200, 200, Bitmap.Config.ARGB_8888));
    }

    public void testICantGetASmallerBitmap() {
        Bitmap original = Bitmap.createBitmap(99, 99, Bitmap.Config.ARGB_8888);
        strategy.put(original);
        assertNull(strategy.get(100, 100, Bitmap.Config.ARGB_8888));
    }

    public void testReturnedDimensionsMatchIfSizeDoesNotMatch() {
        Bitmap original = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        strategy.put(original);
        Bitmap result = strategy.get(99, 99, Bitmap.Config.ARGB_8888);
        assertEquals(99, result.getWidth());
        assertEquals(99, result.getHeight());
    }

    public void testReturnedConfigMatchesIfSizeDoesNotMatch() {
        Bitmap original = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        strategy.put(original);
        Bitmap result = strategy.get(100, 100, Bitmap.Config.RGB_565);
        assertEquals(Bitmap.Config.RGB_565, result.getConfig());
    }

    public void testSmallestMatchingSizeIsReturned() {
        Bitmap smallest = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Bitmap medium = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888);
        Bitmap large = Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888);

        strategy.put(large);
        strategy.put(smallest);
        strategy.put(medium);

        assertEquals(smallest, strategy.get(99, 99, Bitmap.Config.ARGB_8888));
    }

    // This ensures that our sizes are incremented and decremented appropriately so we don't think we have more bitmaps
    // of a size than we actually do.
    public void testAMatchingBitmapIsReturnedIfAvailable() {
        strategy.put(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
        strategy.get(99, 99, Bitmap.Config.ARGB_8888);
        strategy.put(Bitmap.createBitmap(101, 101, Bitmap.Config.ARGB_8888));
        assertNotNull(strategy.get(99, 99, Bitmap.Config.ARGB_8888));
    }

    public void testLeastRecentlyObtainedSizeIsRemovedFirst() {
        Bitmap mostRecentlyUsed = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Bitmap other = Bitmap.createBitmap(1000, 1000, Bitmap.Config.RGB_565);
        Bitmap leastRecentlyUsed = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
        strategy.get(500, 500, Bitmap.Config.ARGB_8888);
        strategy.get(1000, 1000, Bitmap.Config.RGB_565);
        strategy.get(100, 100, Bitmap.Config.ARGB_8888);

        strategy.put(other);
        strategy.put(leastRecentlyUsed);
        strategy.put(mostRecentlyUsed);

        assertEquals(leastRecentlyUsed, strategy.removeLast());
    }
}
