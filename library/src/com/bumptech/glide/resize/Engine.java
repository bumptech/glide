package com.bumptech.glide.resize;

import android.os.Build;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.cache.ResourceCache;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Engine {

    static final boolean CAN_RECYCLE = Build.VERSION.SDK_INT >= 11;
    static final int DEFAULT_DISK_CACHE_SIZE = 250 * 1024 * 1024;

    private final Map<String, ResourceRunner> runners;
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

    public Engine(EngineBuilder builder) {
        this.factory = builder.factory;
        this.cache = builder.cache;
        this.runners = builder.runners;
    }

    Engine(ResourceRunnerFactory factory, ResourceCache cache) {
        this.factory = factory;
        this.cache = cache;
        this.runners = new HashMap<String, ResourceRunner>();
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

        ResourceRunner<Z> runner = factory.build(id, width, height, cacheDecoder, fetcher, decoder, encoder, metadata);
        runners.put(id, runner);
        runner.getJob().addCallback(cb);
        runner.queue();
        return new LoadStatus(runner.getJob());
    }
}
