package com.bumptech.glide.resize;

import com.bumptech.glide.LoadProvider;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;

import java.io.InputStream;

public class FixedLoadProvider<A, T, Z> implements LoadProvider<A, T, Z>  {
    private final ModelLoader<A, T> modelLoader;
    private final DataLoadProvider<T, Z> dataLoadProvider;

    public FixedLoadProvider(ModelLoader<A, T> modelLoader, DataLoadProvider<T, Z> dataLoadProvider) {
        this.modelLoader = modelLoader;
        this.dataLoadProvider = dataLoadProvider;
    }

    @Override
    public ModelLoader<A, T> getModelLoader() {
        return modelLoader;
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
