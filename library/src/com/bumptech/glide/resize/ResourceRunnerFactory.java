package com.bumptech.glide.resize;

import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.io.InputStream;

interface ResourceRunnerFactory {
    /**
     *
     * @param id
     * @param cacheDecoder
     * @param fetcher
     * @param decoder
     * @param encoder
     * @param metadata
     * @param <T> The type of data the resource will be decoded from.
     * @param <Z> The type of the reosurce that will be decoded.
     * @return
     */
    public <T, Z> ResourceRunner build(String id, int width, int height, ResourceDecoder<InputStream, Z> cacheDecoder,
            ResourceFetcher<T> fetcher, ResourceDecoder<T, Z> decoder, ResourceEncoder<Z> encoder, Metadata metadata);
}
