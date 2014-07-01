package com.bumptech.glide;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;

import java.io.File;

/**
 * @param <T> The type of data the resource will be decoded from.
 * @param <Z> The type of resource that will be decoded.
 */
public interface DataLoadProvider<T, Z> {

    public ResourceDecoder<File, Z> getCacheDecoder();

    public ResourceDecoder<T, Z> getSourceDecoder();

    public Encoder<T> getSourceEncoder();

    public ResourceEncoder<Z> getEncoder();
}
