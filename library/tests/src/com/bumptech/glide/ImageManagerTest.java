package com.bumptech.glide;

import android.graphics.Bitmap;
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
}

