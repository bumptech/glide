package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowBitmap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class LruBitmapPoolTest {
    private static final int MAX_SIZE = 10;
    private MockStrategy strategy;
    private LruBitmapPool pool;

    @Before
    public void setUp() throws Exception {
        strategy = new MockStrategy();
        pool = new LruBitmapPool(MAX_SIZE, strategy);
    }

    @Test
    public void testICanAddAndGetABitmap() {
        fillPool(pool, 1);
        pool.put(createMutableBitmap());
        assertNotNull(pool.get(100, 100, Bitmap.Config.ARGB_8888));
    }

    @Test
    public void testImmutableBitmapsAreNotAdded() {
        Bitmap bitmap = createMutableBitmap();
        Robolectric.shadowOf(bitmap).setMutable(false);
        pool.put(bitmap);
        assertEquals(0, strategy.bitmaps.size());
    }

    @Test
    public void testItIsSizeLimited() {
        fillPool(pool, MAX_SIZE + 2);
        assertEquals(2, strategy.numRemoves);
    }

    @Test
    public void testBitmapLargerThanPoolIsNotAdded() {
        strategy = new MockStrategy() {
            @Override
            public int getSize(Bitmap bitmap) {
                return 4;
            }
        };
        pool = new LruBitmapPool(3, strategy);
        pool.put(createMutableBitmap());
        assertEquals(0, strategy.numRemoves);
        assertEquals(0, strategy.numPuts);
    }

    @Test
    public void testClearMemoryRemovesAllBitmaps() {
        fillPool(pool, MAX_SIZE);
        pool.clearMemory();

        assertEquals(MAX_SIZE, strategy.numRemoves);
    }

    @Test
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

    @Test
    public void testTrimMemoryBackgroundOrLessRemovesHalfOfBitmaps() {
        testTrimMemory(MAX_SIZE, TRIM_MEMORY_BACKGROUND, MAX_SIZE / 2);
    }

    @Test
    public void testTrimMemoryBackgroundOrLessRemovesNoBitmapsIfPoolLessThanHalfFull() {
        testTrimMemory(MAX_SIZE / 2, TRIM_MEMORY_BACKGROUND, 0);
    }

    @Test
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
            pool.put(createMutableBitmap());
        }
    }

    private Bitmap createMutableBitmap() {
        Bitmap bitmap = ShadowBitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Robolectric.shadowOf(bitmap).setMutable(true);
        return bitmap;
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
