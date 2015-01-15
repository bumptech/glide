package com.bumptech.glide.load.model;

import android.content.Context;

import com.bumptech.glide.load.data.DataFetcherSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains an ordered set of {@link ModelLoader}s and the model and data types they handle in order from highest
 * priority to lowe
 */
public class ModelLoaderRegistry {

    private final MultiModelLoaderFactory multiModelLoaderFactory;
    private final ModelLoaderCache cache = new ModelLoaderCache();

    public ModelLoaderRegistry(Context context) {
        this(new MultiModelLoaderFactory(context));
    }

    // Visible for testing.
    ModelLoaderRegistry(MultiModelLoaderFactory multiModelLoaderFactory) {
        this.multiModelLoaderFactory = multiModelLoaderFactory;
    }

    public synchronized <Model, Data> void append(Class<Model> modelClass, Class<Data> dataClass,
            ModelLoaderFactory<Model, Data> factory) {
        multiModelLoaderFactory.append(modelClass, dataClass, factory);
        cache.clear();
    }

    public synchronized <Model, Data> void prepend(Class<Model> modelClass, Class<Data> dataClass,
            ModelLoaderFactory<Model, Data> factory) {
        multiModelLoaderFactory.prepend(modelClass, dataClass, factory);
        cache.clear();
    }

    public synchronized <Model, Data> void remove(Class<Model> modelClass, Class<Data> dataClass) {
        tearDown(multiModelLoaderFactory.remove(modelClass, dataClass));
        cache.clear();
    }

    public synchronized <Model, Data> void replace(Class<Model> modelClass, Class<Data> dataClass,
            ModelLoaderFactory<Model, Data> factory) {
        tearDown(multiModelLoaderFactory.replace(modelClass, dataClass, factory));
        cache.clear();
    }

    private <Model, Data> void tearDown(List<ModelLoaderFactory<Model, Data>> factories) {
        for (ModelLoaderFactory<?, ?> factory : factories) {
            factory.teardown();
        }
    }

    public synchronized <A> DataFetcherSet<?> getDataFetchers(A model, int width, int height) {
        List<ModelLoader<A, ?>> modelLoaders = getModelLoaders(model);

        return new DataFetcherSet<A>(model, width, height, modelLoaders);
    }

    public synchronized <Model, Data> ModelLoader<Model, Data> build(Class<Model> modelClass, Class<Data> dataClass) {
        return multiModelLoaderFactory.build(modelClass, dataClass);
    }

    public synchronized List<Class<?>> getDataClasses(Class<?> modelClass){
        return multiModelLoaderFactory.getDataClasses(modelClass);
    }

    private <A> List<ModelLoader<A, ?>> getModelLoaders(A model) {
        Class<A> modelClass = getClass(model);
        List<ModelLoader<A, ?>> loaders = cache.get(modelClass);
        if (loaders == null) {
            loaders = Collections.unmodifiableList(multiModelLoaderFactory.build(modelClass));
            cache.put(modelClass, loaders);
        }
        return loaders;
    }

    @SuppressWarnings("unchecked")
    private static <A> Class<A> getClass(A model) {
        return (Class<A>) model.getClass();
    }

    private static class ModelLoaderCache {
        private final Map<Class<?>, Entry<?>> cachedModelLoaders = new HashMap<Class<?>, Entry<?>>();

        public void clear() {
            cachedModelLoaders.clear();
        }

        public <Model> void put(Class<Model> modelClass, List<ModelLoader<Model, ?>> loaders) {
            Entry<?> previous = cachedModelLoaders.put(modelClass, new Entry<Model>(loaders));
            if (previous != null) {
                throw new IllegalStateException("Already cached loaders for model: " + modelClass);
            }
        }

        @SuppressWarnings("unchecked")
        public <Model> List<ModelLoader<Model, ?>> get(Class<Model> modelClass) {
            Entry<Model> entry = (Entry<Model>) cachedModelLoaders.get(modelClass);
            return entry == null ? null : entry.loaders;
        }

        private static class Entry<Model> {
            private final List<ModelLoader<Model, ?>> loaders;

            public Entry(List<ModelLoader<Model, ?>> loaders) {
                this.loaders = loaders;
            }
        }
    }
}
