package com.bumptech.glide.resize;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.cache.DiskCache;
import com.bumptech.glide.resize.cache.DiskCacheAdapter;
import com.bumptech.glide.resize.cache.DiskLruCacheWrapper;
import com.bumptech.glide.resize.cache.ResourceCache;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class Engine {

    private static final boolean CAN_RECYCLE = Build.VERSION.SDK_INT >= 11;
    private static final int DEFAULT_DISK_CACHE_SIZE = 250 * 1024 * 1024;

    private final Map<String, ResourceRunner> runners = new HashMap<String, ResourceRunner>();
    private final ResourceRunnerFactory factory;
    private ResourceCache cache;

    public static class LoadStatus {
        private final EngineJob engineJob;

        public LoadStatus(EngineJob engineJob) {
            this.engineJob = engineJob;
        }

        public void cancel() {
            engineJob.cancel();
        }
    }

    public Engine(Context context) {
        final int safeCacheSize = ImageManager.getSafeMemoryCacheSize(context);
        final boolean isLowMemoryDevice = ImageManager.isLowMemoryDevice(context);
        boolean recycleBitmaps = CAN_RECYCLE;
        // On low ram devices we double the default bitmap pool size by default so we decrease
        // the default memory cache size here to compensate.
        this.cache = new ResourceCache(!isLowMemoryDevice && recycleBitmaps ? safeCacheSize / 2 : safeCacheSize);

        DiskCache diskCache = null;
        File cacheDir = ImageManager.getPhotoCacheDir(context);
        if (cacheDir != null) {
            diskCache = DiskLruCacheWrapper.get(cacheDir, DEFAULT_DISK_CACHE_SIZE);
        }
        if (diskCache == null) {
            diskCache = new DiskCacheAdapter();
        }

        ExecutorService resizeService = null;
        if (resizeService == null) {
            final int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
            resizeService = new FifoPriorityThreadPoolExecutor(cores);
        }

        this.factory = new DefaultResourceRunnerFactory(cache, runners, diskCache, new Handler(Looper.getMainLooper()),
                resizeService);
    }


    Engine(ResourceRunnerFactory factory, ResourceCache cache) {
        this.factory = factory;
        this.cache = cache;
    }

    /**
     *
     * @param id A unique id for the model, dimensions, cache decoder, decoder, and encoder
     * @param cacheDecoder
     * @param fetcher
     * @param decoder
     * @param encoder
     * @param metadata
     * @param <T> The type of data the resource will be decoded from.
     * @param <Z> The type of the resource that will be decoded.
     */
    public <T, Z> LoadStatus load(String id, int width, int height, ResourceDecoder<InputStream, Z> cacheDecoder,
            ResourceFetcher<T> fetcher, ResourceDecoder<T, Z> decoder, ResourceEncoder<Z> encoder, Metadata metadata,
            ResourceCallback<Z> cb) {
        Resource<Z> cached = cache.get(id);
        if (cached != null) {
            cb.onResourceReady(cached);
            return null;
        }

        ResourceRunner<Z> current = runners.get(id);
        if (current != null) {
            EngineJob<Z> job = current.getJob();
            job.addCallback(cb);
            return new LoadStatus(job);
        }
        ResourceRunner runner = factory.build(id, width, height, cacheDecoder, fetcher, decoder, encoder, metadata);
        runners.put(id, runner);
        runner.queue();
        return new LoadStatus(runner.getJob());
    }
}
