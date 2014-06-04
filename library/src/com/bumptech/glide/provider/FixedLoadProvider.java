package com.bumptech.glide.provider;

import com.bumptech.glide.DataLoadProvider;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.load.model.ModelLoader;

import java.io.InputStream;

public class FixedLoadProvider<A, T, Z, R> implements LoadProvider<A, T, Z, R>  {
    private final ModelLoader<A, T> modelLoader;
    private ResourceTranscoder<Z, R> transcoder;
    private final DataLoadProvider<T, Z> dataLoadProvider;

    public FixedLoadProvider(ModelLoader<A, T> modelLoader, ResourceTranscoder<Z, R> transcoder,
            DataLoadProvider<T, Z> dataLoadProvider) {
        if (modelLoader == null) {
            throw new NullPointerException("ModelLoader must not be null");
        }
        this.modelLoader = modelLoader;
        if (transcoder == null) {
            throw new NullPointerException("Transcoder must not be null");
        }
        this.transcoder = transcoder;
        if (dataLoadProvider == null) {
            throw new NullPointerException("DataLoadProvider must not be null");
        }
        this.dataLoadProvider = dataLoadProvider;
    }

    @Override
    public ModelLoader<A, T> getModelLoader() {
        return modelLoader;
    }

    @Override
    public ResourceTranscoder<Z, R> getTranscoder() {
        return transcoder;
    }

    @Override
    public ResourceDecoder<InputStream, Z> getCacheDecoder() {
        return dataLoadProvider.getCacheDecoder();
    }

    @Override
    public ResourceDecoder<T, Z> getSourceDecoder() {
        return dataLoadProvider.getSourceDecoder();
    }

    @Override
    public ResourceEncoder<Z> getEncoder() {
        return dataLoadProvider.getEncoder();
    }
}
