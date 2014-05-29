package com.bumptech.glide;

import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.resize.DataLoadProvider;

public interface LoadProvider<A, T, Z> extends DataLoadProvider<T, Z> {

    public ModelLoader<A, T> getModelLoader();
}
