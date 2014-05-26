package com.bumptech.glide.resize;

import android.os.Handler;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.cache.DiskCache;
import com.bumptech.glide.resize.cache.ResourceCache;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;

class DefaultResourceRunnerFactory implements ResourceRunnerFactory {
    private ResourceCache resourceCache;
    private Map<String, ResourceRunner> runners;
    private DiskCache diskCache;
    private Handler mainHandler;
    private ExecutorService service;

    public DefaultResourceRunnerFactory(ResourceCache resourceCache, Map<String, ResourceRunner> runners,
            DiskCache diskCache, Handler mainHandler, ExecutorService service) {
        this.resourceCache = resourceCache;
        this.runners = runners;
        this.diskCache = diskCache;
        this.mainHandler = mainHandler;
        this.service = service;
    }

    /**
     *
     * @param id
     * @param cacheDecoder
     * @param fetcher
     * @param decoder
     * @param encoder
     * @param metadata
     * @param <T> The type of the data the resource will be decoded from.
     * @param <Z> The type of the resource that will be decoded.
     * @return
     */
    @Override
    public <T, Z> ResourceRunner build(String id, int width, int height, ResourceDecoder<InputStream, Z> cacheDecoder,
            ResourceFetcher<T> fetcher, ResourceDecoder<T, Z> decoder, ResourceEncoder<Z> encoder, Metadata metadata) {
        EngineJob<Z> engineJob = new EngineJob<Z>(id, resourceCache, runners, mainHandler);
        SourceResourceRunner<T, Z> sourceRunner = new SourceResourceRunner<T, Z>(id, width, height, fetcher, decoder,
                encoder, diskCache, metadata, engineJob);
        return new ResourceRunner<Z>(id, width, height, diskCache, cacheDecoder, sourceRunner, service, engineJob);
    }
}
