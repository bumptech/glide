package com.bumptech.glide.resize;

import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.cache.DiskCache;

import java.io.OutputStream;

public class SourceResourceRunner<T> implements Runnable, DiskCache.Writer {
    private final String id;
    private final ResourceFetcher<T> resourceFetcher;
    private final ResourceDecoder<T> decoder;
    private final ResourceEncoder<T> encoder;
    private DiskCache diskCache;
    private Metadata metadata;
    private ResourceCallback cb;
    private Resource result;

    public SourceResourceRunner(String id, ResourceFetcher<T> resourceFetcher, ResourceDecoder<T> decoder,
            ResourceEncoder<T> encoder, DiskCache diskCache, Metadata metadata, ResourceCallback cb) {
        this.id = id;
        this.resourceFetcher = resourceFetcher;
        this.decoder = decoder;
        this.encoder = encoder;
        this.diskCache = diskCache;
        this.metadata = metadata;
        this.cb = cb;
    }

    @Override
    public void run() {
        try {
            result = null;
            T toDecode = resourceFetcher.loadResource(metadata);
            if (toDecode != null) {
                result = decoder.decode(toDecode);
            }
            if (result != null) {
                diskCache.put(id, this);
                cb.onResourceReady(result);
            } else {
                cb.onException(null);
            }

        } catch (Exception e) {
            cb.onException(e);
        }
    }

    @Override
    public void write(OutputStream os) {
        encoder.encode(result, os);
    }
}
