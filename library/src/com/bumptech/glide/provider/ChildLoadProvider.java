package com.bumptech.glide.provider;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.model.ModelLoader;

import java.io.InputStream;

public class ChildLoadProvider<A, T, Z> implements LoadProvider<A, T, Z> {
    private LoadProvider<A, T, Z> parent;
    private ResourceDecoder<InputStream, Z> cacheDecoder;
    private ResourceDecoder<T, Z> sourceDecoder;
    private ResourceEncoder<Z> encoder;

    public ChildLoadProvider(LoadProvider<A, T, Z> parent) {
        this.parent = parent;
    }

    @Override
    public ModelLoader<A, T> getModelLoader() {
        return parent.getModelLoader();
    }

    public void setCacheDecoder(ResourceDecoder<InputStream, Z> cacheDecoder) {
        this.cacheDecoder = cacheDecoder;
    }

    public void setSourceDecoder(ResourceDecoder<T, Z> sourceDecoder) {
        this.sourceDecoder = sourceDecoder;
    }

    public void setEncoder(ResourceEncoder<Z> encoder) {
        this.encoder = encoder;
    }

    @Override
    public ResourceDecoder<InputStream, Z> getCacheDecoder() {
        if (cacheDecoder != null) {
            return cacheDecoder;
        } else {
            return parent.getCacheDecoder();
        }
    }

    @Override
    public ResourceDecoder<T, Z> getSourceDecoder() {
        if (sourceDecoder != null) {
            return sourceDecoder;
        } else {
            return parent.getSourceDecoder();
        }
    }

    @Override
    public ResourceEncoder<Z> getEncoder() {
        if (encoder != null) {
            return encoder;
        } else {
            return parent.getEncoder();
        }
    }
}
