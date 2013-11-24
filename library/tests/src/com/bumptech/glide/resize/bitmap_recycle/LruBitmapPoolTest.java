package com.bumptech.glide.resize.bitmap_recycle;

import android.graphics.Bitmap;
import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE;

public class LruBitmapPoolTest extends AndroidTestCase {
    private static final int MAX_SIZE = 10;
    private MockStrategy strategy;
    private LruBitmapPool pool;

    @Override
    protected void setUp() throws Exception {
        strategy = new MockStrategy();
        pool = new LruBitmapPool(MAX_SIZE, strategy);
    }

    public void testICanAddAndGetABitmap() {
        fillPool(pool, 1);
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        pool.put(bitmap);
        assertNotNull(pool.get(100, 100, Bitmap.Config.ARGB_8888));
    }

    public void testImmutableBitmapsAreNotAdded() {
        pool.put(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).copy(Bitmap.Config.ARGB_8888, false));
        assertEquals(0, strategy.bitmaps.size());
    }

    public void testItIsSizeLimited() {
        fillPool(pool, MAX_SIZE + 2);
        assertEquals(2, strategy.numRemoves);
    }

    public void testBitmapLargerThanPoolIsNotAdded() {
        strategy = new MockStrategy() {
            @Override
            public int getSize(Bitmap bitmap) {
                return 4;
            }
        };
        pool = new LruBitmapPool(3, strategy);
        pool.put(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
        assertEquals(0, strategy.numRemoves);
        assertEquals(0, strategy.numPuts);
    }

    public void testClearMemoryRemovesAllBitmaps() {
        fillPool(pool, MAX_SIZE);
        pool.clearMemory();

        assertEquals(MAX_SIZE, strategy.numRemoves);
    }

    public void testEvictedBitmapsAreRecycled() {
        fillPool(pool, MAX_SIZE);
        List<Bitmap> bitmaps = new ArrayList<Bitmap>(MAX_SIZE);
        for (Bitmap b : strategy.bitmaps) {
            bitmaps.add(b);
        }

        pool.clearMemory();

        for (Bitmap b : bitmaps) {
            assertTrue(b.isRecycled());
        }
    }

    public void testTrimMemoryBackgroundOrLessRemovesHalfOfBitmaps() {
        testTrimMemory(MAX_SIZE, TRIM_MEMORY_BACKGROUND, MAX_SIZE / 2);
    }

    public void testTrimMemoryBackgroundOrLessRemovesNoBitmapsIfPoolLessThanHalfFull() {
        testTrimMemory(MAX_SIZE / 2, TRIM_MEMORY_BACKGROUND, 0);
    }

    public void testTrimMemoryModerateOrGreaterRemovesAllBitmaps() {
        for (int trimLevel : new int[] { TRIM_MEMORY_MODERATE, TRIM_MEMORY_COMPLETE }) {
            testTrimMemory(MAX_SIZE, trimLevel, MAX_SIZE);
        }
    }

    private void testTrimMemory(int fillSize, int trimLevel, int expectedSize) {
        MockStrategy strategy = new MockStrategy();
        LruBitmapPool pool = new LruBitmapPool(MAX_SIZE, strategy);
        fillPool(pool, fillSize);
        pool.trimMemory(trimLevel);
        assertEquals("Failed level=" + trimLevel, expectedSize, strategy.numRemoves);
    }

    private void fillPool(LruBitmapPool pool, int fillCount) {
        for (int i = 0; i < fillCount; i++) {
            pool.put(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
        }
    }

    private static final int getSize(Bitmap bitmap) {
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    private static class MockStrategy implements LruPoolStrategy {
        private LinkedList<Bitmap> bitmaps = new LinkedList<Bitmap>();
        private int numRemoves;
        private int numPuts;

        @Override
        public void put(Bitmap bitmap) {
            numPuts++;
            bitmaps.add(bitmap);
        }

        @Override
        public Bitmap get(int width, int height, Bitmap.Config config) {
            return bitmaps.removeLast();
        }

        @Override
        public Bitmap removeLast() {
            numRemoves++;
            return bitmaps.removeLast();
        }

        @Override
        public String logBitmap(Bitmap bitmap) {
            return null;
        }

        @Override
        public String logBitmap(int width, int height, Bitmap.Config config) {
            return null;
        }

        @Override
        public int getSize(Bitmap bitmap) {
            return 1;
        }
    }
}
