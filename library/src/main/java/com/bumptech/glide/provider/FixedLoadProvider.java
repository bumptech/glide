package com.bumptech.glide.provider;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

import java.io.File;

/**
 * A {@link com.bumptech.glide.provider.LoadProvider} that sets the classes it provides using non null arguments in its
 * constructor.
 *
 * @param <A> The type of the model the resource will be loaded from.
 * @param <T> The type of the data that will be retrieved for the model.
 * @param <Z> The type of the resource that will be decoded from the data.
 * @param <R> The type of the resource that will be transcoded from the decoded resource.
 */
public class FixedLoadProvider<A, T, Z, R> implements LoadProvider<A, T, Z, R>  {
    private final ModelLoader<A, T> modelLoader;
    private final ResourceTranscoder<Z, R> transcoder;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelLoader<A, T> getModelLoader() {
        return modelLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTranscoder<Z, R> getTranscoder() {
        return transcoder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceDecoder<File, Z> getCacheDecoder() {
        return dataLoadProvider.getCacheDecoder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceDecoder<T, Z> getSourceDecoder() {
        return dataLoadProvider.getSourceDecoder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Encoder<T> getSourceEncoder() {
        return dataLoadProvider.getSourceEncoder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceEncoder<Z> getEncoder() {
        return dataLoadProvider.getEncoder();
    }
}
