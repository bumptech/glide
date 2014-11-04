package com.bumptech.glide.load.engine.prefill;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
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
import java.util.concurrent.TimeUnit;

/**
 * A class that allocates {@link android.graphics.Bitmap Bitmaps} to make sure that the
 * {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} is pre-populated.
 *
 * <p>By posting to the main thread with backoffs, we try to avoid ANRs when the garbage collector gets into a state
 * where a high percentage of {@link Bitmap} allocations trigger a stop the world GC. We try to detect whether or not a
 * GC has occurred by only allowing our allocator to run for a limited number of milliseconds. Since the allocations
 * themselves very fast, a GC is the most likely reason for a substantial delay. If we detect our allocator has run for
 * more than our limit, we assume a GC has occurred, stop the current allocations, and try again after a delay.
 */
final class BitmapPreFillRunner implements Runnable {
    private static final String TAG = "PreFillRunner";
    private static final Clock DEFAULT_CLOCK = new Clock();

    /**
     * The maximum number of millis we can run before posting. Set to match and detect the duration of non concurrent
     * GCs.
     */
    static final long MAX_DURATION_MS = 32;

    /**
     * The amount of time in ms we wait before continuing to allocate after the first GC is detected.
     */
    static final long INITIAL_BACKOFF_MS = 40;

    /**
     * The amount by which the current backoff time is multiplied each time we detect a GC.
     */
    static final int BACKOFF_RATIO = 4;

    /**
     * The maximum amount of time in ms we wait before continuing to allocate.
     */
    static final long MAX_BACKOFF_MS = TimeUnit.SECONDS.toMillis(1);

    private final BitmapPool bitmapPool;
    private final MemoryCache memoryCache;
    private final PreFillQueue toPrefill;
    private final Clock clock;
    private final Set<PreFillType> seenTypes = new HashSet<PreFillType>();
    private final Handler handler;

    private long currentDelay = INITIAL_BACKOFF_MS;
    private boolean isCancelled;

    public BitmapPreFillRunner(BitmapPool bitmapPool, MemoryCache memoryCache, PreFillQueue allocationOrder) {
        this(bitmapPool, memoryCache, allocationOrder, DEFAULT_CLOCK, new Handler(Looper.getMainLooper()));
    }

    // Visible for testing.
    BitmapPreFillRunner(BitmapPool bitmapPool, MemoryCache memoryCache, PreFillQueue allocationOrder, Clock clock,
            Handler handler) {
        this.bitmapPool = bitmapPool;
        this.memoryCache = memoryCache;
        this.toPrefill = allocationOrder;
        this.clock = clock;
        this.handler = handler;
    }

    public void cancel() {
        isCancelled = true;
    }

    /**
     * Attempts to allocate {@link android.graphics.Bitmap}s and returns {@code true} if there are more
     * {@link android.graphics.Bitmap}s to allocate and {@code false} otherwise.
     */
    private boolean allocate() {
        long start = clock.now();
        while (!toPrefill.isEmpty() && !isGcDetected(start)) {
            PreFillType toAllocate = toPrefill.remove();
            Bitmap bitmap = Bitmap.createBitmap(toAllocate.getWidth(), toAllocate.getHeight(),
                    toAllocate.getConfig());

            // Don't over fill the memory cache to avoid evicting useful resources, but make sure it's not empty so
            // we use all available space.
            if (getFreeMemoryCacheBytes() >= Util.getBitmapByteSize(bitmap)) {
                memoryCache.put(new UniqueKey(), BitmapResource.obtain(bitmap, bitmapPool));
            } else {
                addToBitmapPool(toAllocate, bitmap);
            }

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "allocated [" + toAllocate.getWidth() + "x" + toAllocate.getHeight() + "] "
                        + toAllocate.getConfig() + " size: " + Util.getBitmapByteSize(bitmap));
            }
        }

        return !isCancelled && !toPrefill.isEmpty();
    }

    private boolean isGcDetected(long startTimeMs) {
        return clock.now() - startTimeMs >= MAX_DURATION_MS;
    }

    private int getFreeMemoryCacheBytes() {
        return memoryCache.getMaxSize() - memoryCache.getCurrentSize();
    }

    private void addToBitmapPool(PreFillType toAllocate, Bitmap bitmap) {
        // The pool may not move sizes to the front of the LRU on put. Do a get here to make sure the size we're adding
        // is at the front of the queue so that the Bitmap we're adding won't be evicted immediately.
        if (seenTypes.add(toAllocate)) {
          Bitmap fromPool = bitmapPool.get(toAllocate.getWidth(), toAllocate.getHeight(),
              toAllocate.getConfig());
            if (fromPool != null) {
                bitmapPool.put(fromPool);
            }
        }

        bitmapPool.put(bitmap);
    }

    @Override
    public void run() {
        if (allocate()) {
            handler.postDelayed(this, getNextDelay());
        }
    }

    private long getNextDelay() {
        long result = currentDelay;
        currentDelay = Math.min(currentDelay * BACKOFF_RATIO, MAX_BACKOFF_MS);
        return result;
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
