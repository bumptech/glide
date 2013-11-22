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

    public void testClearMemoryRemovesAllBitmaps() {
        List<Bitmap> bitmaps = fillPool();
        assertTrue(bitmaps.size() >= 2);

        Bitmap first = bitmaps.get(0);
        assertNotNull(pool.get(first.getWidth(), first.getHeight(), first.getConfig()));
        pool.clearMemory();
        assertNull(pool.get(first.getWidth(), first.getHeight(), first.getConfig()));
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

    private static int getSize(Bitmap bitmap) {
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    private static Bitmap getBitmap() {
        return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    }
}
