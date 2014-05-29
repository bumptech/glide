package com.bumptech.glide.resize;

import android.os.Handler;
import android.util.Log;
import com.bumptech.glide.resize.cache.DiskCache;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 *
 * @param <Z> The type of the resource that will be decoded.
 */
public class ResourceRunner<Z> implements Runnable {
    private static final String TAG = "ResourceRunner";

    private final Key key;
    private final SourceResourceRunner sourceRunner;
    private final ExecutorService executorService;
    private final EngineJob<Z> job;
    private final ResourceDecoder<InputStream, Z> cacheDecoder;
    private final int width;
    private final int height;
    private final DiskCache diskCache;
    private final Handler bgHandler;
    private volatile Future<?> future;
    private volatile boolean isCancelled;

    public ResourceRunner(Key key, int width, int height, DiskCache diskCache, ResourceDecoder<InputStream, Z> cacheDecoder,
            SourceResourceRunner sourceRunner, ExecutorService executorService, Handler bgHandler, EngineJob<Z> job) {
        this.key = key;
        this.width = width;
        this.height = height;
        this.diskCache = diskCache;
        this.cacheDecoder = cacheDecoder;
        this.sourceRunner = sourceRunner;
        this.executorService = executorService;
        this.bgHandler = bgHandler;
        this.job = job;
    }

    public EngineJob<Z> getJob() {
        return job;
    }

    public void cancel() {
        isCancelled = true;
        bgHandler.removeCallbacks(this);
        if (future != null) {
            future.cancel(false);
        }
        sourceRunner.cancel();
    }

    public void queue() {
        bgHandler.post(this);
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
                    Log.d(TAG, "Failed to decode image from cache");
                }
                diskCache.delete(key);
            }
        }
        return result;
    }
}
