package com.bumptech.glide.resize;

import com.bumptech.glide.resize.cache.DiskCache;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;

public class ResourceRunner implements Runnable {
    private final String id;
    private final SourceResourceRunner sourceRunner;
    private final ExecutorService executorService;
    private final ResourceCallback cb;
    private final ResourceDecoder<InputStream> cacheDecoder;
    private final DiskCache diskCache;

    public ResourceRunner(String id, DiskCache diskCache, ResourceDecoder<InputStream> cacheDecoder,
            SourceResourceRunner sourceRunner, ExecutorService executorService, ResourceCallback cb) {
        this.id = id;
        this.diskCache = diskCache;
        this.cacheDecoder = cacheDecoder;
        this.sourceRunner = sourceRunner;
        this.executorService = executorService;
        this.cb = cb;
    }

    @Override
    public void run() {
        Resource fromCache = loadFromDiskCache();
        if (fromCache != null) {
            cb.onResourceReady(fromCache);
        } else {
            executorService.submit(sourceRunner);
        }
    }

    private Resource loadFromDiskCache() {
        Resource result = null;
        InputStream fromCache = diskCache.get(id);
        if (fromCache != null) {
            result = cacheDecoder.decode(fromCache);
        }
        return result;
    }

}
