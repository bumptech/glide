package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE;

public class LruBitmapPool implements BitmapPool {
    private static final String TAG = "LruBitmapPool";
    private final LruPoolStrategy strategy;

    private final int initialMaxSize;
    private int maxSize;
    private int currentSize = 0;
    private int hits;
    private int misses;
    private int puts;
    private int evictions;

    // Exposed for testing only.
    LruBitmapPool(int maxSize, LruPoolStrategy strategy) {
        this.initialMaxSize = maxSize;
        this.maxSize = maxSize;
        this.strategy = strategy;
    }

    public LruBitmapPool(int maxSize) {
        initialMaxSize = maxSize;
        this.maxSize = maxSize;
        if (Build.VERSION.SDK_INT >= 19) {
            strategy = new SizeStrategy();
        } else {
            strategy = new AttributeStrategy();
        }
    }

    @Override
    public void setSizeMultiplier(float sizeMultiplier) {
        maxSize = Math.round(initialMaxSize * sizeMultiplier);
        evict();
    }

    @Override
    public synchronized boolean put(Bitmap bitmap) {
        if (!bitmap.isMutable() || strategy.getSize(bitmap) > maxSize) {
            return false;
        }

        final int size = strategy.getSize(bitmap);
        strategy.put(bitmap);

        puts++;
        currentSize += size;

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Put bitmap in pool=" + strategy.logBitmap(bitmap));
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
        final Bitmap result = strategy.get(width, height, config);
        if (result == null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Missing bitmap=" + strategy.logBitmap(width, height, config));
            }
            misses++;
        } else {
            hits++;
            currentSize -= strategy.getSize(result);
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Get bitmap=" + strategy.logBitmap(width, height, config));
        }
        dump();

        return result;
    }

    @Override
    public void clearMemory() {
        trimToSize(0);
    }

    @Override
    public void trimMemory(int level) {
        if (level >= TRIM_MEMORY_MODERATE) {
            clearMemory();
        } else if (level >= TRIM_MEMORY_BACKGROUND) {
            trimToSize(maxSize / 2);
        }
    }

    private void trimToSize(int size) {
        while (currentSize > size) {
            final Bitmap removed = strategy.removeLast();
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
            Log.v(TAG, "Hits=" + hits + " misses=" + misses + " puts=" + puts + " evictions=" + evictions + " currentSize="
                    + currentSize + " maxSize=" + maxSize + "\nStrategy=" + strategy);
        }
    }
}
