package com.bumptech.glide.resize;

import com.bumptech.glide.resize.cache.DiskCache;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 *
 * @param <Z> The type of the resource that will be decoded.
 */
public class ResourceRunner<Z> implements Runnable {
    private final String id;
    private final SourceResourceRunner sourceRunner;
    private final ExecutorService executorService;
    private final EngineJob<Z> job;
    private final ResourceDecoder<InputStream, Z> cacheDecoder;
    private final int width;
    private final int height;
    private final DiskCache diskCache;

    private volatile Future<?> future;
    private volatile boolean isCancelled;

    public ResourceRunner(String id, int width, int height, DiskCache diskCache, ResourceDecoder<InputStream, Z> cacheDecoder,
            SourceResourceRunner sourceRunner, ExecutorService executorService, EngineJob<Z> job) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.diskCache = diskCache;
        this.cacheDecoder = cacheDecoder;
        this.sourceRunner = sourceRunner;
        this.executorService = executorService;
        this.job = job;
    }

    public EngineJob<Z> getJob() {
        return job;
    }

    public void cancel() {
        isCancelled = true;
        future.cancel(false);
        sourceRunner.cancel();
    }

    public void queue() {
        future = executorService.submit(this);
    }

    @Override
    public void run() {
        if (isCancelled) {
            return;
        }

        Resource<Z> fromCache = loadFromDiskCache();
        if (fromCache != null) {
            job.onResourceReady(fromCache);
        } else {
            future = executorService.submit(sourceRunner);
        }
    }

    private Resource<Z> loadFromDiskCache() {
        Resource<Z> result = null;
        InputStream fromCache = diskCache.get(id);
        if (fromCache != null) {
            result = cacheDecoder.decode(fromCache, width, height);
        }
        return result;
    }

}
