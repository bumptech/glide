package com.bumptech.glide.resize;

import android.os.Build;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.cache.ResourceCache;
import com.bumptech.glide.resize.load.Transformation;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Engine implements EngineJobListener, ResourceCache.ResourceRemovedListener {

    static final boolean CAN_RECYCLE = Build.VERSION.SDK_INT >= 11;
    static final int DEFAULT_DISK_CACHE_SIZE = 250 * 1024 * 1024;

    private final Map<String, ResourceRunner> runners;
    private final ResourceRunnerFactory factory;
    private final ResourceReferenceCounter resourceReferenceCounter;
    private ResourceCache cache;

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
        this(builder.factory, builder.cache, new HashMap<String, ResourceRunner>(), builder.resourceReferenceCounter);
    }

    Engine(ResourceRunnerFactory factory, ResourceCache cache, Map<String, ResourceRunner> runners,
            ResourceReferenceCounter referenceCounter) {
        this.factory = factory;
        this.cache = cache;
        this.runners = runners;
        this.resourceReferenceCounter = referenceCounter;

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
    public <T, Z> LoadStatus load(String id, int width, int height,  ResourceDecoder<InputStream, Z> cacheDecoder,
            ResourceFetcher<T> fetcher, ResourceDecoder<T, Z> decoder,  Transformation<Z> transformation,
            ResourceEncoder<Z> encoder, Metadata metadata, ResourceCallback<Z> cb) {
        Resource<Z> cached = cache.get(id);
        if (cached != null) {
            cb.onResourceReady(cached);
            return null;
        }

        ResourceRunner<Z> current = runners.get(id);
        if (current != null) {
            EngineJob<Z> job = current.getJob();
            job.addCallback(cb);
            return new LoadStatus(cb, job);
        }

        ResourceRunner<Z> runner = factory.build(id, width, height, cacheDecoder, fetcher, decoder, transformation,
                encoder, metadata, this, cb);
        runners.put(id, runner);
        runner.queue();
        return new LoadStatus(cb, runner.getJob());
    }

    @Override
    public void onEngineJobComplete(String id) {
        runners.remove(id);
    }

    @Override
    public void onEngineJobCancelled(String id) {
        ResourceRunner runner = runners.remove(id);
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
