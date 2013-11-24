package com.bumptech.glide;

import android.content.ComponentCallbacks2;
import android.test.AndroidTestCase;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.resize.cache.MemoryCache;
import com.bumptech.glide.resize.cache.MemoryCacheAdapter;

import java.util.concurrent.atomic.AtomicInteger;

public class ImageManagerTest extends AndroidTestCase {

    public void testClearMemory() {
        final AtomicInteger clearsCalled = new AtomicInteger();
        // The pool's clear method must be called after the cache's so that the bitmaps from the cache do not just
        // refill the pool
        BitmapPool bitmapPool = new BitmapPoolAdapter() {
            @Override
            public void clearMemory() {
                assertEquals(2, clearsCalled.incrementAndGet());
            }
        };
        MemoryCache memoryCache = new MemoryCacheAdapter() {
            @Override
            public void clearMemory() {
                super.clearMemory();
                assertEquals(1, clearsCalled.incrementAndGet());
            }
        };

        ImageManager im = new ImageManager.Builder(getContext())
                .setBitmapPool(bitmapPool)
                .setMemoryCache(memoryCache)
                .build();

        im.clearMemory();

        assertEquals(2, clearsCalled.get());
    }

    public void testTrimMemory() {
        final AtomicInteger trimsCalled = new AtomicInteger();
        BitmapPool pool = new BitmapPoolAdapter() {
            @Override
            public void trimMemory(int level) {
                super.trimMemory(level);
                assertEquals(2, trimsCalled.incrementAndGet());
            }
        };

        MemoryCache cache = new MemoryCacheAdapter() {
            @Override
            public void trimMemory(int level) {
                super.trimMemory(level);
                assertEquals(1, trimsCalled.incrementAndGet());
            }
        };

        ImageManager im = new ImageManager.Builder(getContext())
                .setBitmapPool(pool)
                .setMemoryCache(cache)
                .build();


        im.trimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE);
        assertEquals(2, trimsCalled.get());
    }
}

