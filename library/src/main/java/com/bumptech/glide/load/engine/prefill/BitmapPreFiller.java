package com.bumptech.glide.load.engine.prefill;

import android.os.Looper;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.util.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * A class for pre-filling {@link android.graphics.Bitmap Bitmaps} in a
 * {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool}.
 */
public final class BitmapPreFiller {

    private final MemoryCache memoryCache;
    private final BitmapPool bitmapPool;

    private BitmapPreFillIdleHandler current;

    public BitmapPreFiller(MemoryCache memoryCache, BitmapPool bitmapPool) {
        this.memoryCache = memoryCache;
        this.bitmapPool = bitmapPool;
    }

    public void preFill(PreFillBitmapAttribute... bitmapAttributes) {
        if (current != null) {
            current.cancel();
        }
        PreFillQueue allocationOrder = generateAllocationOrder(bitmapAttributes);
        current = new BitmapPreFillIdleHandler(bitmapPool, memoryCache, allocationOrder);
        Looper.myQueue().addIdleHandler(current);
    }

    // Visible for testing.
    PreFillQueue generateAllocationOrder(PreFillBitmapAttribute[] preFillSizes) {
        final int maxSize = (memoryCache.getMaxSize() - memoryCache.getCurrentSize()) + bitmapPool.getMaxSize();

        int totalWeight = 0;
        for (PreFillBitmapAttribute size : preFillSizes) {
            totalWeight += size.getWeight();
        }

        final float bytesPerWeight = maxSize / (float) totalWeight;

        Map<PreFillBitmapAttribute, Integer> attributeToCount = new HashMap<PreFillBitmapAttribute, Integer>();
        for (PreFillBitmapAttribute size : preFillSizes) {
            int bytesForSize = Math.round(bytesPerWeight * size.getWeight());
            int bytesPerBitmap = getSizeInBytes(size);
            int bitmapsForSize = bytesForSize / bytesPerBitmap;
            attributeToCount.put(size, bitmapsForSize);
        }

        return new PreFillQueue(attributeToCount);
    }

    private static int getSizeInBytes(PreFillBitmapAttribute size) {
        return Util.getBitmapPixelSize(size.getWidth(), size.getHeight(), size.getConfig());
    }
}

