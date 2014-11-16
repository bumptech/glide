package com.bumptech.glide.load.model;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.DataFetcherSet;

import java.util.List;

// TODO: more cleanly split generic loader factory and this class.
public class ModelLoaderRegistry {

    private GenericLoaderFactory factory;

    public ModelLoaderRegistry(GenericLoaderFactory factory) {
        this.factory = factory;
    }

    public <A> DataFetcherSet getDataFetchers(A model, int width, int height) {
        List<ModelLoader<A, ?>> modelLoaders = getModelLoaders(model);

        DataFetcherSet fetcherSet = new DataFetcherSet();
        for (ModelLoader<A, ?> modelLoader : modelLoaders) {
            DataFetcher<?> fetcher = modelLoader.getDataFetcher(model, width, height);
            fetcherSet.add(fetcher);
        }
        return fetcherSet;
    }

    @SuppressWarnings("unchecked")
    private <A> List<ModelLoader<A, ?>> getModelLoaders(A model) {
        return factory.buildModelLoaders((Class<A>) model.getClass());
    }
}
