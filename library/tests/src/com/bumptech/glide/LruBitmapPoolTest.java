package com.bumptech.glide;

import android.graphics.Bitmap;
import android.test.AndroidTestCase;
import com.bumptech.glide.resize.bitmap_recycle.LruBitmapPool;

import java.util.ArrayList;
import java.util.List;

public class LruBitmapPoolTest extends AndroidTestCase {
    private static final int SIZE = 1024 * 1024;
    private LruBitmapPool pool;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        pool = new LruBitmapPool(SIZE);
    }

    public void testCanAddAndRemoveBitmap() {
        Bitmap bitmap = getBitmap();
        pool.put(bitmap);
        assertEquals(bitmap, getEquivalentFromPool(bitmap));
    }

    public void testCanAddAndRemoveBitmapsOfDifferentSizes() {
        Bitmap first = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Bitmap second = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
        pool.put(first);
        pool.put(second);
        assertEquals(first, getEquivalentFromPool(first));
        assertEquals(second, getEquivalentFromPool(second));
    }

    public void testCanAddAndRemoveBitmapsOfDifferentConfigs() {
        Bitmap first = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Bitmap second = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
        pool.put(first);
        pool.put(second);
        assertEquals(first, getEquivalentFromPool(first));
        assertEquals(second, getEquivalentFromPool(second));
    }

    public void testPoolIsSizeLimited() {
        List<Bitmap> bitmaps = fillPool();
        Bitmap first = bitmaps.get(0);
        pool.put(Bitmap.createBitmap(first));

        int totalInPool = 0;
        for (int i = 0; i < bitmaps.size(); i++) {
            if (getEquivalentFromPool(first) == null) {
                break;
            }
            totalInPool++;
        }

        assertEquals(bitmaps.size(), totalInPool);
    }

    public void testLeastRecentlyAcquiredBitmapRemovedFirst() {
        Bitmap special = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
        pool.put(Bitmap.createBitmap(special));
        pool.put(Bitmap.createBitmap(special));
        getEquivalentFromPool(special);
        List<Bitmap> bitmaps = fillPool();

        assertNotNull(getEquivalentFromPool(special));

        Bitmap first = bitmaps.get(0);
        int totalAcquired = 0;
        for (int i = 0; i < bitmaps.size(); i++) {
            if (getEquivalentFromPool(first) == null) {
                break;
            }
            totalAcquired++;
        }

        assertEquals(totalAcquired, bitmaps.size() - 1);
    }

    public void testClearMemoryRemovesAllBitmaps() {
        List<Bitmap> bitmaps = fillPool();
        assertTrue(bitmaps.size() >= 2);

        Bitmap first = bitmaps.get(0);
        assertNotNull(getEquivalentFromPool(first));
        pool.clearMemory();
        assertNull(getEquivalentFromPool(first));
    }

    public void testClearMemoryCallsRecycleOnRemovedBitmaps() {
        List<Bitmap> bitmaps = fillPool();
        pool.clearMemory();
        for (Bitmap bitmap : bitmaps) {
            assertTrue(bitmap.isRecycled());
        }
    }

    public List<Bitmap> fillPool() {
        List<Bitmap> bitmaps = new ArrayList<Bitmap>();
        Bitmap toPut = getBitmap();
        int bitmapSize = getSize(toPut);
        for (int i = 0; i < (SIZE / bitmapSize); i++) {
            bitmaps.add(Bitmap.createBitmap(toPut));
        }
        for (Bitmap bitmap : bitmaps) {
            pool.put(bitmap);
        }
        assertTrue(bitmaps.size() > 0);
        return bitmaps;
    }

    private Bitmap getEquivalentFromPool(Bitmap bitmap) {
        return pool.get(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
    }

    private static int getSize(Bitmap bitmap) {
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    private static Bitmap getBitmap() {
        return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    }
}
