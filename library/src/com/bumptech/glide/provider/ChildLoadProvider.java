package com.bumptech.glide.provider;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.load.model.ModelLoader;

import java.io.InputStream;

public class ChildLoadProvider<A, T, Z, R> implements LoadProvider<A, T, Z, R> {
    private LoadProvider<A, T, Z, R> parent;
    private ResourceDecoder<InputStream, Z> cacheDecoder;
    private ResourceDecoder<T, Z> sourceDecoder;
    private ResourceEncoder<Z> encoder;
    private ResourceTranscoder<Z, R> transcoder;

    public ChildLoadProvider(LoadProvider<A, T, Z, R> parent) {
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

    public void setTranscoder(ResourceTranscoder<Z, R> transcoder) {
        this.transcoder = transcoder;
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

    @Override
    public ResourceTranscoder<Z, R> getTranscoder() {
        if (transcoder != null) {
            return transcoder;
        } else {
            return parent.getTranscoder();
        }
    }

}
