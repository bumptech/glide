package com.bumptech.glide.resize;

import android.os.Build;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.cache.ResourceCache;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Engine implements EngineJobListener {

    static final boolean CAN_RECYCLE = Build.VERSION.SDK_INT >= 11;
    static final int DEFAULT_DISK_CACHE_SIZE = 250 * 1024 * 1024;

    private final Map<String, ResourceRunner> runners;
    private final ResourceRunnerFactory factory;
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
        this.factory = builder.factory;
        this.cache = builder.cache;
        this.runners = new HashMap<String, ResourceRunner>();
    }

    Engine(ResourceRunnerFactory factory, ResourceCache cache, Map<String, ResourceRunner> runners) {
        this.factory = factory;
        this.cache = cache;
        this.runners = runners;
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
            return new LoadStatus(cb, job);
        }

        ResourceRunner<Z> runner = factory.build(id, width, height, cacheDecoder, fetcher, decoder, encoder, metadata,
                this, cb);
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
}
