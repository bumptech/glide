package com.bumptech.glide.resize;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import com.bumptech.glide.resize.cache.DiskCache;
import com.bumptech.glide.resize.cache.DiskCacheAdapter;
import com.bumptech.glide.resize.cache.DiskLruCacheWrapper;
import com.bumptech.glide.resize.cache.ResourceCache;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EngineBuilder {
    private ExecutorService service;
    private Context context;
    DefaultResourceRunnerFactory factory;
    ResourceCache cache;
    private Handler bgHandler;

    public EngineBuilder(Context context) {
        this.context = context;
    }

    public EngineBuilder setExecutorService(ExecutorService service) {
        this.service = service;
        return this;
    }

    public EngineBuilder setBackgroundHandler(Handler bgHandler) {
        this.bgHandler = bgHandler;
        return this;
    }

    public Engine build() {
        final int safeCacheSize = ImageManager.getSafeMemoryCacheSize(context);
        final boolean isLowMemoryDevice = ImageManager.isLowMemoryDevice(context);
        boolean recycleBitmaps = Engine.CAN_RECYCLE;
        // On low ram devices we double the default bitmap pool size by default so we decrease
        // the default memory cache size here to compensate.
        cache = new ResourceCache(!isLowMemoryDevice && recycleBitmaps ? safeCacheSize / 2 : safeCacheSize);

        DiskCache diskCache = null;
        File cacheDir = ImageManager.getPhotoCacheDir(context);
        if (cacheDir != null) {
            diskCache = DiskLruCacheWrapper.get(cacheDir, Engine.DEFAULT_DISK_CACHE_SIZE);
        }
        if (diskCache == null) {
            diskCache = new DiskCacheAdapter();
        }

        if (service == null) {
            //TODO: re-enabled prioritization.
            final int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
//            service = new FifoPriorityThreadPoolExecutor(cores);
            service = Executors.newFixedThreadPool(cores);
        }

        if (bgHandler == null) {
            HandlerThread handlerThread = new HandlerThread("EngineThread");
            handlerThread.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            handlerThread.start();
            bgHandler = new Handler(handlerThread.getLooper());
        }

        factory = new DefaultResourceRunnerFactory(cache, diskCache, new Handler(Looper.getMainLooper()), service,
                bgHandler);

        return new Engine(this);
    }

}
