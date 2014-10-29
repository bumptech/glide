package com.bumptech.glide.load.engine.prefill;

import android.graphics.Bitmap;
import android.os.MessageQueue;
import android.os.SystemClock;
import android.util.Log;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.util.Util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

/**
 * A class that allocates {@link android.graphics.Bitmap Bitmaps} when the main thread runs out of messages so that the
 * {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} is pre-populated.
 */
final class BitmapPreFillIdleHandler implements MessageQueue.IdleHandler {
    private static final String TAG = "PreFillIdleHandler";
    // Visisble for testing.
    static final long MAX_DURATION_MILLIS = 32;

    private static final Clock DEFAULT_CLOCK = new Clock();

    private final BitmapPool bitmapPool;
    private final MemoryCache memoryCache;
    private final PreFillQueue toPrefill;
    private final Clock clock;
    private final Set<PreFillType> seenAttributes =
        new HashSet<PreFillType>();

    private boolean isCancelled;

    public BitmapPreFillIdleHandler(BitmapPool bitmapPool, MemoryCache memoryCache,
            PreFillQueue allocationOrder) {
        this(bitmapPool, memoryCache, allocationOrder, DEFAULT_CLOCK);
    }

    // Visible for testing.
    BitmapPreFillIdleHandler(BitmapPool bitmapPool, MemoryCache memoryCache,
            PreFillQueue allocationOrder, Clock clock) {
        this.bitmapPool = bitmapPool;
        this.memoryCache = memoryCache;
        this.toPrefill = allocationOrder;
        this.clock = clock;
    }

    public void cancel() {
        isCancelled = true;
    }

    @Override
    public boolean queueIdle() {
        long start = clock.now();
        while (!toPrefill.isEmpty() && (clock.now() - start) < MAX_DURATION_MILLIS) {
            PreFillType toAllocate = toPrefill.remove();
            Bitmap bitmap = Bitmap.createBitmap(toAllocate.getWidth(), toAllocate.getHeight(),
                    toAllocate.getConfig());

            // Don't over fill the memory cache to avoid evicting useful resources, but make sure it's not empty so
            // we use all available space.
            if ((memoryCache.getMaxSize() - memoryCache.getCurrentSize()) >= Util.getBitmapByteSize(bitmap)) {
                memoryCache.put(new UniqueKey(), BitmapResource.obtain(bitmap, bitmapPool));
            } else {
                if (seenAttributes.add(toAllocate)) {
                  Bitmap fromPool = bitmapPool.get(toAllocate.getWidth(), toAllocate.getHeight(),
                      toAllocate.getConfig());
                    if (fromPool != null) {
                        bitmapPool.put(fromPool);
                    }
                }
                bitmapPool.put(bitmap);
            }

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "allocated [" + toAllocate.getWidth() + "x" + toAllocate.getHeight() + "] "
                        + toAllocate.getConfig() + " size: " + Util.getBitmapByteSize(bitmap));
            }
        }

        return !isCancelled && !toPrefill.isEmpty();
    }

    private static class UniqueKey implements Key {

        @Override
        public void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException {
            // Do nothing.
        }
    }

    // Visible for testing.
    static class Clock {
        public long now() {
            return SystemClock.currentThreadTimeMillis();
        }
    }
}
