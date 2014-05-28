package com.bumptech.glide.resize;

import android.os.Handler;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.cache.DiskCache;
import com.bumptech.glide.resize.cache.MemoryCache;
import com.bumptech.glide.resize.load.Transformation;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;

class DefaultResourceRunnerFactory implements ResourceRunnerFactory {
    private final Handler bgHandler;
    private final ResourceReferenceCounter referenceCounter;
    private MemoryCache memoryCache;
    private DiskCache diskCache;
    private Handler mainHandler;
    private ExecutorService service;

    public DefaultResourceRunnerFactory(MemoryCache memoryCache, DiskCache diskCache, Handler mainHandler,
            ExecutorService service, Handler bgHandler, ResourceReferenceCounter referenceCounter) {
        this.memoryCache = memoryCache;
        this.diskCache = diskCache;
        this.mainHandler = mainHandler;
        this.service = service;
        this.bgHandler = bgHandler;
        this.referenceCounter = referenceCounter;
    }

    @Override
    public <T, Z> ResourceRunner<Z> build(Key key, int width, int height,
            ResourceDecoder<InputStream, Z> cacheDecoder, ResourceFetcher<T> fetcher, ResourceDecoder<T, Z> decoder,
            Transformation<Z> transformation, ResourceEncoder<Z> encoder, Metadata metadata,
            EngineJobListener listener) {

        EngineJob<Z> engineJob = new EngineJob<Z>(key, memoryCache, mainHandler, referenceCounter, listener);

        SourceResourceRunner<T, Z> sourceRunner = new SourceResourceRunner<T, Z>(key, width, height, fetcher, decoder,
                transformation, encoder, diskCache, metadata, engineJob);

        return new ResourceRunner<Z>(key, width, height, diskCache, cacheDecoder, sourceRunner, service, bgHandler,
                engineJob);
    }
}
