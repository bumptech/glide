package com.bumptech.glide.provider;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

import java.io.File;

/**
 * A {@link com.bumptech.glide.provider.LoadProvider} that returns classes preferentially from those set on it but
 * that also defaults to a wrapped {@link com.bumptech.glide.provider.LoadProvider} when a particular class is not set.
 *
 * @param <A> The type of the model the resource will be loaded from.
 * @param <T> The type of the data that will be retrieved for the model.
 * @param <Z> The type of the resource that will be decoded from the data.
 * @param <R> The type of the resource that will be transcoded from the decoded resource.
 */
public class ChildLoadProvider<A, T, Z, R> implements LoadProvider<A, T, Z, R>, Cloneable {
    private final LoadProvider<A, T, Z, R> parent;

    private ResourceDecoder<File, Z> cacheDecoder;
    private ResourceDecoder<T, Z> sourceDecoder;
    private ResourceEncoder<Z> encoder;
    private ResourceTranscoder<Z, R> transcoder;
    private Encoder<T> sourceEncoder;

    public ChildLoadProvider(LoadProvider<A, T, Z, R> parent) {
        this.parent = parent;
    }

    @Override
    public ModelLoader<A, T> getModelLoader() {
        return parent.getModelLoader();
    }

    /**
     * Sets the {@link com.bumptech.glide.load.ResourceDecoder} to use for decoding the resource from the disk cache.
     *
     * @param cacheDecoder The decoder to use.
     */
    public void setCacheDecoder(ResourceDecoder<File, Z> cacheDecoder) {
        this.cacheDecoder = cacheDecoder;
    }

    /**
     * Sets the {@link com.bumptech.glide.load.ResourceDecoder} to use to decoding the resource from the original data.
     *
     * @param sourceDecoder The decoder to use.
     */
    public void setSourceDecoder(ResourceDecoder<T, Z> sourceDecoder) {
        this.sourceDecoder = sourceDecoder;
    }

    /**
     * Sets the {@link com.bumptech.glide.load.ResourceEncoder} to use to write the decoded and transformed resource to
     * the disk cache.
     *
     * @param encoder The encoder to use.
     */
    public void setEncoder(ResourceEncoder<Z> encoder) {
        this.encoder = encoder;
    }

    /**
     * Sets the {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder} to use to transcode the decoded
     * resource.
     *
     * @param transcoder The transcoder to use.
     */
    public void setTranscoder(ResourceTranscoder<Z, R> transcoder) {
        this.transcoder = transcoder;
    }

    /**
     * Sets the {@link com.bumptech.glide.load.Encoder} to use to write the original data to the disk cache.
     *
     * @param sourceEncoder The encoder to use.
     */
    public void setSourceEncoder(Encoder<T> sourceEncoder) {
        this.sourceEncoder = sourceEncoder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceDecoder<File, Z> getCacheDecoder() {
        if (cacheDecoder != null) {
            return cacheDecoder;
        } else {
            return parent.getCacheDecoder();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceDecoder<T, Z> getSourceDecoder() {
        if (sourceDecoder != null) {
            return sourceDecoder;
        } else {
            return parent.getSourceDecoder();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Encoder<T> getSourceEncoder() {
        if (sourceEncoder != null) {
            return sourceEncoder;
        } else {
            return parent.getSourceEncoder();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceEncoder<Z> getEncoder() {
        if (encoder != null) {
            return encoder;
        } else {
            return parent.getEncoder();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceTranscoder<Z, R> getTranscoder() {
        if (transcoder != null) {
            return transcoder;
        } else {
            return parent.getTranscoder();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public ChildLoadProvider<A, T, Z, R> clone() {
        try {
            return (ChildLoadProvider<A, T, Z, R>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
