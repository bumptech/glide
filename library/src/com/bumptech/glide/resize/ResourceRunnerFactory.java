package com.bumptech.glide.resize;

import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.load.Transformation;

import java.io.InputStream;

interface ResourceRunnerFactory {
    /**
     *
     * @param key
     * @param cacheDecoder
     * @param fetcher
     * @param decoder
     * @param transformation
     * @param encoder
     * @param metadata
     * @param <T> The type of data the resource will be decoded from.
     * @param <Z> The type of the reosurce that will be decoded.
     * @return
     */
    public <T, Z> ResourceRunner<Z> build(Key key, int width, int height,
            ResourceDecoder<InputStream, Z> cacheDecoder, ResourceFetcher<T> fetcher, ResourceDecoder<T, Z> decoder,
            Transformation<Z> transformation,  ResourceEncoder<Z> encoder, Metadata metadata,
            EngineJobListener listener);
}
