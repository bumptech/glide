package com.bumptech.glide.load.engine;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.load.data.DataFetcher;

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
     * @param <Z> The type of the resource that will be decoded.
     * @param <R> The type of the resource that will be transcoded to from the decoded resource.
     * @return
     */
    public <T, Z, R> ResourceRunner<Z, R> build(Key key, int width, int height,
            ResourceDecoder<InputStream, Z> cacheDecoder, DataFetcher<T> fetcher, ResourceDecoder<T, Z> decoder,
            Transformation<Z> transformation,  ResourceEncoder<Z> encoder, ResourceTranscoder<Z, R> transcoder,
            Priority priority, boolean isMemoryCacheable, EngineJobListener listener);
}
