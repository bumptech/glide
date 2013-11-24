package com.bumptech.glide.resize.bitmap_recycle;

import android.graphics.Bitmap;
import android.test.AndroidTestCase;

public class AttributeStrategyTest extends AndroidTestCase {

    private AttributeStrategy strategy;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        strategy = new AttributeStrategy();
    }

    public void testIGetNullIfNoMatchingBitmapExists() {
        assertNull(strategy.get(100, 100, Bitmap.Config.ARGB_8888));
    }

    public void testICanAddAndGetABitmapOfTheSameSizeAndDimensions() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        strategy.put(bitmap);
        assertEquals(bitmap, strategy.get(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888));
    }

    public void testICantGetABitmapOfTheSameDimensionsButDifferentConfigs() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        strategy.put(bitmap);
        assertNull(strategy.get(100, 100, Bitmap.Config.RGB_565));
    }

    public void testICantGetABitmapOfTheSameDimensionsAndSizeButDifferentConfigs() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_4444);
        strategy.put(bitmap);
        assertNull(strategy.get(100, 100, Bitmap.Config.RGB_565));
    }

    public void testICantGetABitmapOfDifferentWidths() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        strategy.put(bitmap);
        assertNull(strategy.get(99, 100, Bitmap.Config.ARGB_8888));
    }

    public void testICantGetABitmapOfDifferentHeights() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        strategy.put(bitmap);
        assertNull(strategy.get(100, 99, Bitmap.Config.ARGB_8888));
    }

    public void testICantGetABitmapOfDifferentDimensionsButTheSameSize() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        strategy.put(bitmap);
        assertNull(strategy.get(50, 200, Bitmap.Config.ARGB_8888));
    }

    public void testMultipleBitmapsOfDifferentAttributesCanBeAddedAtOnce() {
        Bitmap first = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
        Bitmap second = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Bitmap third = Bitmap.createBitmap(120, 120, Bitmap.Config.RGB_565);

        strategy.put(first);
        strategy.put(second);
        strategy.put(third);

        assertEquals(first, strategy.get(100, 100, Bitmap.Config.RGB_565));
        assertEquals(second, strategy.get(100, 100, Bitmap.Config.ARGB_8888));
        assertEquals(third, strategy.get(120, 120, Bitmap.Config.RGB_565));
    }

    public void testLeastRecentlyUsedAttributeSetIsRemovedFirst() {
        final Bitmap leastRecentlyUsed = Bitmap.createBitmap(100, 100, Bitmap.Config.ALPHA_8);
        final Bitmap other = Bitmap.createBitmap(1000, 1000, Bitmap.Config.RGB_565);
        final Bitmap mostRecentlyUsed = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

        strategy.get(100, 100, Bitmap.Config.ALPHA_8);
        strategy.get(1000, 1000, Bitmap.Config.RGB_565);
        strategy.get(100, 100, Bitmap.Config.ARGB_8888);

        strategy.put(other);
        strategy.put(leastRecentlyUsed);
        strategy.put(mostRecentlyUsed);

        Bitmap removed = strategy.removeLast();
        assertEquals("Expected=" + strategy.logBitmap(leastRecentlyUsed) + " got=" + strategy.logBitmap(removed),
                leastRecentlyUsed, removed);
    }
}
