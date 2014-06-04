package com.bumptech.glide.provider;

import com.bumptech.glide.DataLoadProvider;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.load.model.ModelLoader;

/**
 * @param <A> The type of model.
 * @param <T> The type of data that will be decoded from.
 * @param <Z> The type of resource that will be decoded.
 * @param <R> The type of resource that the decoded resource will be transcoded to.
 */
public interface LoadProvider<A, T, Z, R> extends DataLoadProvider<T, Z> {

    public ModelLoader<A, T> getModelLoader();

    public ResourceTranscoder<Z, R> getTranscoder();
}
