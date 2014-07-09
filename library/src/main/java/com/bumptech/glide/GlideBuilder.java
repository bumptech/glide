package com.bumptech.glide;

import android.content.Context;
import android.os.Build;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskCacheAdapter;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.load.engine.executor.FifoPriorityThreadPoolExecutor;

import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 * A builder class for setting default structural classes for Glide to use.
 */
public class GlideBuilder {
    private Context context;
    private Engine engine;
    private BitmapPool bitmapPool;
    private MemoryCache memoryCache;
    private DiskCache diskCache;
    private ExecutorService resizeService;
    private ExecutorService diskCacheService;

    public GlideBuilder(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Sets the {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} implementation to use to store and
     * retrieve reused {@link android.graphics.Bitmap}s.
     *
     * @param bitmapPool The pool to use.
     * @return This builder.
     */
    public GlideBuilder setBitmapPool(BitmapPool bitmapPool) {
        this.bitmapPool = bitmapPool;
        return this;
    }

    /**
     * Sets the {@link com.bumptech.glide.load.engine.cache.MemoryCache} implementation to store
     * {@link com.bumptech.glide.load.engine.Resource}s that are not currently in use.
     *
     * @param memoryCache  The cache to use.
     * @return This builder.
     */
    public GlideBuilder setMemoryCache(MemoryCache memoryCache) {
        this.memoryCache = memoryCache;
        return this;
    }

    /**
     * Sets the {@link com.bumptech.glide.load.engine.cache.DiskCache} implementation to use to store
     * {@link com.bumptech.glide.load.engine.Resource} data and thumbnails.
     *
     * @param diskCache The disk cache to use.
     * @return This builder.
     */
    public GlideBuilder setDiskCache(DiskCache diskCache) {
        this.diskCache = diskCache;
        return this;
    }

    /**
     * Sets the {@link java.util.concurrent.ExecutorService} implementation to use when retrieving
     * {@link com.bumptech.glide.load.engine.Resource}s that are not already in the cache.
     *
     * <p>
     *     Any implementation must order requests based on their {@link com.bumptech.glide.Priority} for thumbnail
     *     requests to work properly.
     * </p>
     *
     * @see #setDiskCacheService(java.util.concurrent.ExecutorService)
     * @see com.bumptech.glide.load.engine.executor.FifoPriorityThreadPoolExecutor
     *
     * @param service The ExecutorService to use.
     * @return This builder.
     */
    public GlideBuilder setResizeService(ExecutorService service) {
        this.resizeService = service;
        return this;
    }

    /**
     * Sets the {@link java.util.concurrent.ExecutorService} implementation to use when retrieving
     * {@link com.bumptech.glide.load.engine.Resource}s that are currently in cache.
     *
     * <p>
     *     Any implementation must order requests based on their {@link com.bumptech.glide.Priority} for thumbnail
     *     requests to work properly.
     * </p>
     *
     * @see #setResizeService(java.util.concurrent.ExecutorService)
     * @see com.bumptech.glide.load.engine.executor.FifoPriorityThreadPoolExecutor
     *
     * @param service The ExecutorService to use.
     * @return This builder.
     */
    public GlideBuilder setDiskCacheService(ExecutorService service) {
        this.diskCacheService = service;
        return this;
    }

    // For testing.
    GlideBuilder setEngine(Engine engine) {
        this.engine = engine;
        return this;
    }

    Glide createGlide() {
        if (resizeService == null) {
            final int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
            resizeService = new FifoPriorityThreadPoolExecutor(cores);
        }
        if (diskCacheService == null) {
            diskCacheService = new FifoPriorityThreadPoolExecutor(1);
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
            engine = new Engine(memoryCache, diskCache, resizeService, diskCacheService);
        }

        return new Glide(engine, memoryCache, bitmapPool, context);
    }
}