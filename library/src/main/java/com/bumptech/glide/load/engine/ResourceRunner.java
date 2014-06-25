package com.bumptech.glide.load.engine;

import android.os.SystemClock;
import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.executor.Prioritized;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 *
 * @param <Z> The type of the resource that will be decoded.
 * @param <R> the type of the resource the decoded resource will be transcoded to.
 */
public class ResourceRunner<Z, R> implements Runnable, Prioritized {
    private static final String TAG = "ResourceRunner";

    private final Key key;
    private final Transformation<Z> transformation;
    private final ResourceTranscoder<Z, R> transcoder;
    private final SourceResourceRunner sourceRunner;
    private final EngineJob job;
    private final Priority priority;
    private final ResourceDecoder<InputStream, Z> cacheDecoder;
    private final int width;
    private final int height;
    private final DiskCache diskCache;
    private final ExecutorService diskCacheService;
    private final ExecutorService resizeService;
    private volatile Future<?> future;
    private volatile boolean isCancelled;

    public ResourceRunner(Key key, int width, int height, DiskCache diskCache,
            ResourceDecoder<InputStream, Z> cacheDecoder, Transformation<Z> transformation,
            ResourceTranscoder<Z, R> transcoder, SourceResourceRunner sourceRunner, ExecutorService diskCacheService,
            ExecutorService resizeService, EngineJob job, Priority priority) {
        this.key = key;
        this.width = width;
        this.height = height;
        this.diskCache = diskCache;
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

        long start = SystemClock.currentThreadTimeMillis();
        Resource<Z> fromCache = loadFromDiskCache();
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "loaded from disk cache in " + (SystemClock.currentThreadTimeMillis() - start));
        }
        if (fromCache != null) {
            Resource<Z> transformed = transformation.transform(fromCache, width, height);
            if (transformed != fromCache) {
                fromCache.recycle();
            }
            Resource<R> transcoded = transcoder.transcode(transformed);
            job.onResourceReady(transcoded);
        } else {
            future = resizeService.submit(sourceRunner);
        }
    }

    private Resource<Z> loadFromDiskCache() {
        Resource<Z> result = null;
        InputStream fromCache = diskCache.get(key);
        if (fromCache != null) {
            try {
                result = cacheDecoder.decode(fromCache, width, height);
            } catch (IOException e) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Exception decoding image from cache", e);
                }
            }
            if (result == null) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Failed to decode image from cache or not present in cache");
                }
                diskCache.delete(key);
            }
        }
        return result;
    }

    @Override
    public int getPriority() {
        return priority.ordinal();
    }
}
