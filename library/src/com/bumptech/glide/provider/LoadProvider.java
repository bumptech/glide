package com.bumptech.glide.provider;

import com.bumptech.glide.DataLoadProvider;
import com.bumptech.glide.load.model.ModelLoader;

public interface LoadProvider<A, T, Z> extends DataLoadProvider<T, Z> {

    public ModelLoader<A, T> getModelLoader();
}
