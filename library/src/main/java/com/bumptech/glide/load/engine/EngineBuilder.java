package com.bumptech.glide.load.engine;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.bumptech.glide.load.engine.executor.FifoPriorityThreadPoolExecutor;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;

import java.util.concurrent.ExecutorService;

public class EngineBuilder {
    final MemoryCache memoryCache;
    final DiskCache diskCache;

    private ExecutorService diskCacheService;
    private ExecutorService resizeService;
    private Handler bgHandler;

    ResourceRunnerFactory factory;
    KeyFactory keyFactory;

    public EngineBuilder(MemoryCache memoryCache, DiskCache diskCache) {
        this.memoryCache = memoryCache;
        this.diskCache = diskCache;
    }

    public EngineBuilder setResizeService(ExecutorService service) {
        resizeService = service;
        return this;
    }

    public EngineBuilder setDiskCacheService(ExecutorService service) {
        diskCacheService = service;
        return this;
    }

    public EngineBuilder setBackgroundHandler(Handler bgHandler) {
        this.bgHandler = bgHandler;
        return this;
    }

    public Engine build() {
        if (resizeService == null) {
            final int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
            resizeService = new FifoPriorityThreadPoolExecutor(cores);
        }
        if (diskCacheService == null) {
            diskCacheService = new FifoPriorityThreadPoolExecutor(1);
        }

        if (bgHandler == null) {
            HandlerThread handlerThread = new HandlerThread("EngineThread") {
                @Override
                public void run() {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                    super.run();
                }
            };
            handlerThread.start();
            bgHandler = new Handler(handlerThread.getLooper(), new Handler.Callback() {
                @Override
                public boolean handleMessage(Message message) {
                    if (message.what == 0) {
                        ((Runnable) message.obj).run();
                        return true;
                    }
                    return false;
                }
            });
        }


        keyFactory = new EngineKeyFactory();

        factory = new DefaultResourceRunnerFactory(memoryCache, diskCache, new Handler(Looper.getMainLooper()),
                diskCacheService, resizeService);

        return new Engine(this);
    }
}
