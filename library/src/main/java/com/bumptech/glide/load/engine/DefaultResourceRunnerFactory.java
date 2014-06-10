package com.bumptech.glide.load.engine;

import android.os.Handler;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;

class DefaultResourceRunnerFactory implements ResourceRunnerFactory {
    private final Handler bgHandler;
    private MemoryCache memoryCache;
    private DiskCache diskCache;
    private Handler mainHandler;
    private ExecutorService service;

    public DefaultResourceRunnerFactory(MemoryCache memoryCache, DiskCache diskCache, Handler mainHandler,
            ExecutorService service, Handler bgHandler ) {
        this.memoryCache = memoryCache;
        this.diskCache = diskCache;
        this.mainHandler = mainHandler;
        this.service = service;
        this.bgHandler = bgHandler;
    }

    @Override
    public <T, Z, R> ResourceRunner<Z, R> build(Key key, int width, int height,
            ResourceDecoder<InputStream, Z> cacheDecoder, DataFetcher<T> fetcher, ResourceDecoder<T, Z> decoder,
            Transformation<Z> transformation, ResourceEncoder<Z> encoder, ResourceTranscoder<Z, R> transcoder,
            Priority priority, boolean isMemoryCacheable, EngineJobListener listener) {

        EngineJob engineJob = new EngineJob(key, memoryCache, mainHandler, isMemoryCacheable, listener);

        SourceResourceRunner<T, Z, R> sourceRunner = new SourceResourceRunner<T, Z, R>(key, width, height, fetcher,
                decoder, transformation, encoder, transcoder, diskCache, priority, engineJob);

        return new ResourceRunner<Z, R>(key, width, height, diskCache, cacheDecoder, transcoder, sourceRunner, service,
                bgHandler, engineJob);
    }
}
