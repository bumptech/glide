package com.bumptech.glide.load.engine.bitmap_recycle;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} implementation that uses an
 * {@link com.bumptech.glide.load.engine.bitmap_recycle.LruPoolStrategy} to bucket {@link Bitmap}s and then uses an LRU
 * eviction policy to evict {@link android.graphics.Bitmap}s from the least recently used bucket in order to keep
 * the pool below a given maximum size limit.
 */
public class LruBitmapPool implements BitmapPool {
    private static final String TAG = "LruBitmapPool";
    private static final Bitmap.Config DEFAULT_CONFIG = Bitmap.Config.ARGB_8888;

    private final LruPoolStrategy strategy;
    private final int initialMaxSize;
    private final BitmapTracker tracker;

    private int maxSize;
    private int currentSize;
    private int hits;
    private int misses;
    private int puts;
    private int evictions;

    // Exposed for testing only.
    LruBitmapPool(int maxSize, LruPoolStrategy strategy) {
        this.initialMaxSize = maxSize;
        this.maxSize = maxSize;
        this.strategy = strategy;
        this.tracker = new NullBitmapTracker();
    }

    /**
     * Constructor for LruBitmapPool.
     *
     * @param maxSize The initial maximum size of the pool in bytes.
     */
    public LruBitmapPool(int maxSize) {
        this(maxSize, getDefaultStrategy());
    }

    @Override
    public int getMaxSize() {
        return maxSize;
    }

    @Override
    public synchronized void setSizeMultiplier(float sizeMultiplier) {
        maxSize = Math.round(initialMaxSize * sizeMultiplier);
        evict();
    }

    @Override
    public synchronized boolean put(Bitmap bitmap) {
        if (!bitmap.isMutable() || strategy.getSize(bitmap) > maxSize) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Reject bitmap from pool=" + strategy.logBitmap(bitmap) + " is mutable="
                        + bitmap.isMutable());
            }
            return false;
        }

        final int size = strategy.getSize(bitmap);
        strategy.put(bitmap);
        tracker.add(bitmap);

        puts++;
        currentSize += size;

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Put bitmap in pool=" + strategy.logBitmap(bitmap));
        }
        dump();

        evict();
        return true;
    }

    private void evict() {
        trimToSize(maxSize);
    }

    @Override
    public synchronized Bitmap get(int width, int height, Bitmap.Config config) {
        Bitmap result = getDirty(width, height, config);
        if (result != null) {
            // Bitmaps in the pool contain random data that in some cases must be cleared for an image to be rendered
            // correctly. we shouldn't force all consumers to independently erase the contents individually, so we do so
            // here. See issue #131.
            result.eraseColor(Color.TRANSPARENT);
        }

        return result;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @Override
    public synchronized Bitmap getDirty(int width, int height, Bitmap.Config config) {
        // Config will be null for non public config types, which can lead to transformations naively passing in
        // null as the requested config here. See issue #194.
        final Bitmap result = strategy.get(width, height, config != null ? config : DEFAULT_CONFIG);
        if (result == null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Missing bitmap=" + strategy.logBitmap(width, height, config));
            }
            misses++;
        } else {
            hits++;
            currentSize -= strategy.getSize(result);
            tracker.remove(result);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                result.setHasAlpha(true);
            }
        }
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Get bitmap=" + strategy.logBitmap(width, height, config));
        }
        dump();

        return result;
    }

    @Override
    public void clearMemory() {
        trimToSize(0);
    }

    @SuppressLint("InlinedApi")
    @Override
    public void trimMemory(int level) {
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            clearMemory();
        } else if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            trimToSize(maxSize / 2);
        }
    }

    private synchronized void trimToSize(int size) {
        while (currentSize > size) {
            final Bitmap removed = strategy.removeLast();
            tracker.remove(removed);
            currentSize -= strategy.getSize(removed);
            removed.recycle();
            evictions++;
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Evicting bitmap=" + strategy.logBitmap(removed));
            }
            dump();
        }
    }

    private void dump() {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Hits=" + hits + " misses=" + misses + " puts=" + puts + " evictions=" + evictions
                    + " currentSize=" + currentSize + " maxSize=" + maxSize + "\nStrategy=" + strategy);
        }
    }

    private static LruPoolStrategy getDefaultStrategy() {
        final LruPoolStrategy strategy;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            strategy = new SizeStrategy();
        } else {
            strategy = new AttributeStrategy();
        }
        return strategy;
    }

    private interface BitmapTracker {
        void add(Bitmap bitmap);
        void remove(Bitmap bitmap);
    }

    @SuppressWarnings("unused")
    // Only used for debugging
    private static class ThrowingBitmapTracker implements BitmapTracker {
        private final Set<Bitmap> bitmaps = Collections.synchronizedSet(new HashSet<Bitmap>());

        @Override
        public void add(Bitmap bitmap) {
            if (bitmaps.contains(bitmap)) {
                throw new IllegalStateException("Can't add already added bitmap: " + bitmap + " [" + bitmap.getWidth()
                        + "x" + bitmap.getHeight() + "]");
            }
            bitmaps.add(bitmap);
        }

        @Override
        public void remove(Bitmap bitmap) {
            if (!bitmaps.contains(bitmap)) {
                throw new IllegalStateException("Cannot remove bitmap not in tracker");
            }
            bitmaps.remove(bitmap);
        }
    }

    private static class NullBitmapTracker implements BitmapTracker {
        @Override
        public void add(Bitmap bitmap) {
            // Do nothing.
        }

        @Override
        public void remove(Bitmap bitmap) {
            // Do nothing.
        }
    }
}
