package com.bumptech.glide.load.engine;

import android.os.SystemClock;
import android.util.Log;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.executor.Prioritized;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * A background {@link java.lang.Runnable} responsible for loading a {@link Resource}
 * from the disk cache and for starting an {@link com.bumptech.glide.load.engine.SourceResourceRunner} if the
 * {@link Resource} is not found in the disk cache.
 *
 * @param <Z> The type of the resource that will be decoded.
 * @param <R> the type of the resource the decoded resource will be transcoded to.
 */
class ResourceRunner<Z, R> implements Runnable, Prioritized {
    private static final String TAG = "ResourceRunner";

    private final EngineKey key;
    private final Transformation<Z> transformation;
    private final ResourceTranscoder<Z, R> transcoder;
    private final SourceResourceRunner<?, Z, R> sourceRunner;
    private final EngineJob job;
    private final Priority priority;
    private final ResourceDecoder<File, Z> cacheDecoder;
    private final int width;
    private final int height;
    private final CacheLoader cacheLoader;
    private final ExecutorService diskCacheService;
    private final ExecutorService resizeService;
    private volatile Future<?> future;
    private volatile boolean isCancelled;

    public ResourceRunner(EngineKey key, int width, int height, CacheLoader cacheLoader,
            ResourceDecoder<File, Z> cacheDecoder, Transformation<Z> transformation,
            ResourceTranscoder<Z, R> transcoder, SourceResourceRunner<?, Z, R> sourceRunner,
            ExecutorService diskCacheService, ExecutorService resizeService, EngineJob job, Priority priority) {
        this.key = key;
        this.width = width;
        this.height = height;
        this.cacheLoader = cacheLoader;
        this.cacheDecoder = cacheDecoder;
        this.transformation = transformation;
        this.transcoder = transcoder;
        this.sourceRunner = sourceRunner;
        this.diskCacheService = diskCacheService;
        this.resizeService = resizeService;
        this.job = job;
        this.priority = priority;
    }

    public EngineJob getJob() {
        return job;
    }

    public void cancel() {
        isCancelled = true;
        if (future != null) {
            future.cancel(false);
        }
        sourceRunner.cancel();
    }

    public void queue() {
        future = diskCacheService.submit(this);
    }

    @Override
    public void run() {
        if (isCancelled) {
            return;
        }

        Resource<R> result = null;
        try {
            result = runWrapped();
        } catch (Exception e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to decode resource from cache", e);
            }
        }

        if (result != null) {
            job.onResourceReady(result);
        } else {
            submitSourceRunner();
        }
    }

    private Resource<R> runWrapped() {
        long start = SystemClock.currentThreadTimeMillis();
        Resource<Z> fromCache = cacheLoader.load(key, cacheDecoder, width, height);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "loaded from disk cache in " + (SystemClock.currentThreadTimeMillis() - start));
        }

        Resource<R> transcoded = null;
        if (fromCache != null) {
            Resource<Z> transformed = transformation.transform(fromCache, width, height);
            if (transformed != fromCache) {
                fromCache.recycle();
            }
            transcoded = transcoder.transcode(transformed);
        }
        return transcoded;
    }

    private void submitSourceRunner() {
        try {
            future = resizeService.submit(sourceRunner);
        } catch (Exception e) {
            // A variety of exceptions can be thrown here, particularly related to when/if the pool is shutdown.
            job.onException(e);
        }
    }

    @Override
    public int getPriority() {
        return priority.ordinal();
    }
}
