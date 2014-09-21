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

/**
 * A class that allocates {@link android.graphics.Bitmap Bitmaps} when the main thread runs out of messages so that the
 * {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} is pre-populated.
 */
final class BitmapPreFillIdleHandler implements MessageQueue.IdleHandler {
    private static final String TAG = "PreFillIdleHandler";
    // Visisble for testing.
    static final long MAX_DURATION_MILLIS = 32;

    private static final Clock DEFAULT_CLOCK = new DefaultClock();

    private final BitmapPool bitmapPool;
    private final MemoryCache memoryCache;
    private final PreFillQueue toPrefill;
    private final Clock clock;

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
            PreFillBitmapAttribute toAllocate = toPrefill.remove();
            Bitmap bitmap = Bitmap.createBitmap(toAllocate.getWidth(), toAllocate.getHeight(),
                    toAllocate.getConfig());

            // Don't over fill the memory cache to avoid evicting useful resources, but make sure it's not empty so
            // we use all available space.
            if ((memoryCache.getMaxSize() - memoryCache.getCurrentSize()) >= Util.getSize(bitmap)) {
                memoryCache.put(new UniqueKey(), new BitmapResource(bitmap, bitmapPool));
            } else {
                bitmapPool.put(bitmap);
            }

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "allocated [" + toAllocate.getWidth() + "x" + toAllocate.getHeight() + "] "
                        + toAllocate.getConfig() + " size: " + Util.getSize(bitmap));
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
    interface Clock {
        public long now();
    }

    private static class DefaultClock implements Clock {
        @Override
        public long now() {
            return SystemClock.currentThreadTimeMillis();
        }
    }
}
