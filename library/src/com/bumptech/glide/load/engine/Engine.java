package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.resource.ResourceFetcher;
import com.bumptech.glide.Metadata;
import com.bumptech.glide.Resource;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.Transformation;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Engine implements EngineJobListener, MemoryCache.ResourceRemovedListener {

    private final Map<Key, ResourceRunner> runners;
    private final ResourceRunnerFactory factory;
    private final ResourceReferenceCounter resourceReferenceCounter;
    private KeyFactory keyFactory;
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

    public Engine(EngineBuilder builder) {
        this(builder.factory, builder.memoryCache, new HashMap<Key, ResourceRunner>(),
                builder.resourceReferenceCounter, builder.keyFactory);
    }

    Engine(ResourceRunnerFactory factory, MemoryCache cache, Map<Key, ResourceRunner> runners,
            ResourceReferenceCounter referenceCounter, KeyFactory keyFactory) {
        this.factory = factory;
        this.cache = cache;
        this.runners = runners;
        this.resourceReferenceCounter = referenceCounter;
        this.keyFactory = keyFactory;

        cache.setResourceRemovedListener(this);
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
            ResourceFetcher<T> fetcher, ResourceDecoder<T, Z> decoder,  Transformation<Z> transformation,
            ResourceEncoder<Z> encoder, Metadata metadata, ResourceCallback<Z> cb) {

        Key key = keyFactory.buildKey(id, width, height, cacheDecoder, decoder, transformation, encoder);

        Resource<Z> cached = cache.get(key);
        if (cached != null) {
            resourceReferenceCounter.acquireResource(cached);
            cb.onResourceReady(cached);
            return null;
        }

        ResourceRunner<Z> current = runners.get(key);
        if (current != null) {
            EngineJob<Z> job = current.getJob();
            job.addCallback(cb);
            return new LoadStatus(cb, job);
        }

        ResourceRunner<Z> runner = factory.build(key, width, height, cacheDecoder, fetcher, decoder, transformation,
                encoder, metadata, this);
        runner.getJob().addCallback(cb);
        runners.put(key, runner);
        runner.queue();
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
        recycle(resource);
    }

    public void recycle(Resource resource) {
        resourceReferenceCounter.releaseResource(resource);
    }
}
