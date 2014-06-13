package com.bumptech.glide.load.engine;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.load.engine.cache.DiskCache;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 *
 * @param <Z> The type of the resource that will be decoded.
 * @param <R> the type of the resource the decoded resource will be transcoded to.
 */
public class ResourceRunner<Z, R> implements Runnable {
    private static final String TAG = "ResourceRunner";

    private final Key key;
    private ResourceTranscoder<Z, R> transcoder;
    private final SourceResourceRunner sourceRunner;
    private final ExecutorService executorService;
    private final EngineJob job;
    private final ResourceDecoder<InputStream, Z> cacheDecoder;
    private final int width;
    private final int height;
    private final DiskCache diskCache;
    private final Handler bgHandler;
    private volatile Future<?> future;
    private volatile boolean isCancelled;

    public ResourceRunner(Key key, int width, int height, DiskCache diskCache,
            ResourceDecoder<InputStream, Z> cacheDecoder, ResourceTranscoder<Z, R> transcoder,
            SourceResourceRunner sourceRunner, ExecutorService executorService, Handler bgHandler, EngineJob job) {
        this.key = key;
        this.width = width;
        this.height = height;
        this.diskCache = diskCache;
        this.cacheDecoder = cacheDecoder;
        this.transcoder = transcoder;
        this.sourceRunner = sourceRunner;
        this.executorService = executorService;
        this.bgHandler = bgHandler;
        this.job = job;
    }

    public EngineJob getJob() {
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

        long start = SystemClock.currentThreadTimeMillis();
        Resource<Z> fromCache = loadFromDiskCache();
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "loaded from disk cache in " + (SystemClock.currentThreadTimeMillis() - start));
        }
        if (fromCache != null) {
            Resource<R> transcoded = transcoder.transcode(fromCache);
            job.onResourceReady(transcoded);
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
                    Log.d(TAG, "Failed to decode image from cache or not present in cache");
                }
                diskCache.delete(key);
            }
        }
        return result;
    }
}
