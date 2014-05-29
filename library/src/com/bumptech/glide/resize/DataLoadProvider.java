package com.bumptech.glide.resize;

import java.io.InputStream;

public interface DataLoadProvider<T, Z> {

    public ResourceDecoder<InputStream, Z> getCacheDecoder();

    public ResourceDecoder<T, Z> getSourceDecoder();

    public ResourceEncoder<Z> getEncoder();
}
