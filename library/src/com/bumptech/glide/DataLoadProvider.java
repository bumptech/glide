package com.bumptech.glide;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;

import java.io.InputStream;

public interface DataLoadProvider<T, Z> {

    public ResourceDecoder<InputStream, Z> getCacheDecoder();

    public ResourceDecoder<T, Z> getSourceDecoder();

    public ResourceEncoder<Z> getEncoder();
}
