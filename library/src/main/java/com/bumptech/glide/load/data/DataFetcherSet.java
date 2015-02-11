package com.bumptech.glide.load.data;

import com.bumptech.glide.load.model.ModelLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A wrapper for set of {@link com.bumptech.glide.load.data.DataFetcher}s that can fetch data for a given
 * model.
 *
 * @param <Model> The type of model that will be used to retrieve {@link com.bumptech.glide.load.data.DataFetcher}s.
 */
public class DataFetcherSet<Model> implements Iterable<DataFetcher<?>> {

    private final Model model;
    private final int width;
    private final int height;
    private final List<ModelLoader<Model, ?>> modelLoaders;
    private final List<DataFetcher<?>> fetchers;
    private volatile String id;

    public DataFetcherSet(Model model, int width, int height, List<ModelLoader<Model, ?>> modelLoaders) {
        this.model = model;
        this.width = width;
        this.height = height;
        this.modelLoaders = modelLoaders;
        fetchers = new ArrayList<>(modelLoaders.size());
    }

    public boolean isEmpty() {
        return modelLoaders.isEmpty();
    }

    public String getId() {
        if (id == null) {
            for (DataFetcher<?> fetcher : this) {
                if (fetcher != null) {
                    id = fetcher.getId();
                    break;
                }
            }
        }
        return id;
    }

    public void cancel() {
        for (DataFetcher<?> fetcher : fetchers) {
            fetcher.cancel();
        }
    }

    @Override
    public Iterator<DataFetcher<?>> iterator() {
        return new DataFetcherIterator();
    }

    @Override
    public String toString() {
        return "DataFetcherSet{"
                + "fetchers=" + Arrays.toString(fetchers.toArray(new DataFetcher[fetchers.size()]))
                + ", modelLoaders=" + Arrays.toString(modelLoaders.toArray(new ModelLoader[modelLoaders.size()]))
                + "}";
    }

    private class DataFetcherIterator implements Iterator<DataFetcher<?>> {
        int currentIndex;

        @Override
        public boolean hasNext() {
            return currentIndex < modelLoaders.size();
        }

        @Override
        public DataFetcher<?> next() {
            final DataFetcher<?> next;
            if (currentIndex < fetchers.size()) {
                next = fetchers.get(currentIndex);
            } else {
                // We want to cache ModelClass -> [ModelLoader], so we may end up with some ModelLoaders here that
                // don't want to handle our specific Model. We filter those ModelLoaders out here.
                ModelLoader<Model, ?> modelLoader = modelLoaders.get(currentIndex);
                if (modelLoader.handles(model)) {
                    next = modelLoader.getDataFetcher(model, width, height);
                } else {
                    next = null;
                }
                fetchers.add(next);
            }
            currentIndex++;
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
