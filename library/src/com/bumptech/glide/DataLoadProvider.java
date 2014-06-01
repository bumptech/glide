package com.bumptech.glide;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;

import java.io.InputStream;

/**
 * @param <T> The type of data the resource will be decoded from.
 * @param <Z> The type of resource that will be decoded.
 */
public interface DataLoadProvider<T, Z> {

    public ResourceDecoder<InputStream, Z> getCacheDecoder();

    public ResourceDecoder<T, Z> getSourceDecoder();

    public ResourceEncoder<Z> getEncoder();
}
