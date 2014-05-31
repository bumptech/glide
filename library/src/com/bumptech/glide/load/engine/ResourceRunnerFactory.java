package com.bumptech.glide.load.engine;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.resource.ResourceFetcher;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;

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
     * @param priority
     * @param <T> The type of data the resource will be decoded from.
     * @param <Z> The type of the reosurce that will be decoded.
     * @return
     */
    public <T, Z> ResourceRunner<Z> build(Key key, int width, int height,
            ResourceDecoder<InputStream, Z> cacheDecoder, ResourceFetcher<T> fetcher, ResourceDecoder<T, Z> decoder,
            Transformation<Z> transformation,  ResourceEncoder<Z> encoder, Priority priority,
            EngineJobListener listener);
}
