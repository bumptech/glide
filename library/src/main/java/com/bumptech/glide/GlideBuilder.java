package com.bumptech.glide;

import android.content.Context;
import android.os.Build;
import com.android.volley.RequestQueue;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.EngineBuilder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskCacheAdapter;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.volley.RequestQueueWrapper;

import java.io.File;

public class GlideBuilder {
    private RequestQueue requestQueue;
    private Context context;
    private Engine engine;
    private BitmapPool bitmapPool;
    private MemoryCache memoryCache;
    private DiskCache diskCache;

    public GlideBuilder(Context context) {
        this.context = context.getApplicationContext();
    }

    public GlideBuilder setRequestQueue(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
        return this;
    }

    public GlideBuilder setBitmapPool(BitmapPool bitmapPool) {
        this.bitmapPool = bitmapPool;
        return this;
    }

    public GlideBuilder setMemoryCache(MemoryCache memoryCache) {
        this.memoryCache = memoryCache;
        return this;
    }

    public GlideBuilder setDiskCache(DiskCache diskCache) {
        this.diskCache = diskCache;
        return this;
    }

    GlideBuilder setEngine(Engine engine) {
        this.engine = engine;
        return this;
    }

    Glide createGlide() {
        if (requestQueue == null) {
            requestQueue = RequestQueueWrapper.getRequestQueue(context);
        }

        MemorySizeCalculator calculator = new MemorySizeCalculator(context);
        if (bitmapPool == null) {
            if (Build.VERSION.SDK_INT >= 11) {
                bitmapPool = new LruBitmapPool(calculator.getBitmapPoolSize());
            } else {
                bitmapPool = new BitmapPoolAdapter();
            }
        }

        if (memoryCache == null) {
            memoryCache = new LruResourceCache(calculator.getMemoryCacheSize());
        }

        if (diskCache == null) {
            File cacheDir = Glide.getPhotoCacheDir(context);
            if (cacheDir != null) {
                diskCache = DiskLruCacheWrapper.get(cacheDir, Glide.DEFAULT_DISK_CACHE_SIZE);
            }
            if (diskCache == null) {
                diskCache = new DiskCacheAdapter();
            }
        }

        if (engine == null) {
            engine = new EngineBuilder(memoryCache, diskCache)
                    .build();
        }

        return new Glide(engine, requestQueue, memoryCache, bitmapPool, context);
    }
}