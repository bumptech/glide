package com.bumptech.glide.load.engine;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.util.LogTime;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class Engine implements EngineJobListener, MemoryCache.ResourceRemovedListener {
    private static final String TAG = "Engine";

    private final Map<Key, ResourceRunner> runners;
    private final ResourceRunnerFactory factory;
    private final KeyFactory keyFactory;
    private final MemoryCache cache;

    public static class LoadStatus {
        private final EngineJob engineJob;
        private final ResourceCallback cb;

        public LoadStatus(ResourceCallback cb, EngineJob engineJob) {
            this.cb = cb;
            this.engineJob = engineJob;
        }

        public void cancel() {
            engineJob.removeCallback(cb);
        }
    }

    public Engine(MemoryCache memoryCache, DiskCache diskCache, ExecutorService resizeService,
            ExecutorService diskCacheService) {
        this(null, memoryCache, diskCache, resizeService, diskCacheService, null, null);
    }

    Engine(ResourceRunnerFactory factory, MemoryCache cache, DiskCache diskCache, ExecutorService resizeService,
            ExecutorService diskCacheService, Map<Key, ResourceRunner> runners, KeyFactory keyFactory) {
        this.cache = cache;

        if (keyFactory == null) {
            keyFactory = new EngineKeyFactory();
        }
        this.keyFactory = keyFactory;
        if (runners == null) {
            runners = new HashMap<Key, ResourceRunner>();
        }
        this.runners = runners;
        if (factory == null) {
            factory = new DefaultResourceRunnerFactory(cache, diskCache, new Handler(Looper.getMainLooper()),
                    diskCacheService, resizeService);
        }
        this.factory = factory;

        cache.setResourceRemovedListener(this);
    }

    /**
     *
     * @param id A unique id for the model, dimensions, cache decoder, decoder, and encoder
     * @param cacheDecoder
     * @param fetcher
     * @param decoder
     * @param encoder
     * @param transcoder
     * @param priority
     * @param <T> The type of data the resource will be decoded from.
     * @param <Z> The type of the resource that will be decoded.
     * @param <R> The type of the resource that will be transcoded from the decoded resource.
     */
    public <T, Z, R> LoadStatus load(String id, int width, int height, ResourceDecoder<InputStream, Z> cacheDecoder,
            DataFetcher<T> fetcher, ResourceDecoder<T, Z> decoder,  Transformation<Z> transformation,
            ResourceEncoder<Z> encoder, ResourceTranscoder<Z, R> transcoder, Priority priority,
            boolean isMemoryCacheable, ResourceCallback cb) {
        long startTime = LogTime.getLogTime();

        Key key = keyFactory.buildKey(id, width, height, cacheDecoder, decoder, transformation, encoder, transcoder);

        Resource cached = cache.get(key);
        if (cached != null) {
            cached.acquire(1);
            cb.onResourceReady(cached);
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "loaded resource from cache in " + LogTime.getElapsedMillis(startTime));
            }
            return null;
        }

        ResourceRunner current = runners.get(key);
        if (current != null) {
            EngineJob job = current.getJob();
            job.addCallback(cb);
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "added to existing load in " + LogTime.getElapsedMillis(startTime));
            }
            return new LoadStatus(cb, job);
        }

        long start = LogTime.getLogTime();
        ResourceRunner<Z, R> runner = factory.build(key, width, height, cacheDecoder, fetcher, decoder, transformation,
                encoder, transcoder, priority, isMemoryCacheable, this);
        runner.getJob().addCallback(cb);
        runners.put(key, runner);
        runner.queue();
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "queued new load in " + LogTime.getElapsedMillis(start));
            Log.v(TAG, "finished load in engine in " + LogTime.getElapsedMillis(startTime));
        }
        return new LoadStatus(cb, runner.getJob());
    }

    @Override
    public void onEngineJobComplete(Key key) {
        runners.remove(key);
    }

    @Override
    public void onEngineJobCancelled(Key key) {
        ResourceRunner runner = runners.remove(key);
        runner.cancel();
    }

    @Override
    public void onResourceRemoved(Resource resource) {
        resource.release();
    }
}
