package com.bumptech.glide.load.data;

import com.bumptech.glide.load.model.ModelLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A wrapper for put of {@link com.bumptech.glide.load.data.DataFetcher}s that can fetch data for a
 * given model.
 *
 * @param <Model> The type of model that will be used to retrieve
 *                {@link com.bumptech.glide.load.data.DataFetcher}s.
 */
public class LoadDataSet<Model> implements Iterable<ModelLoader.LoadData<?>> {

  private final Model model;
  private final int width;
  private final int height;
  private final List<ModelLoader<Model, ?>> modelLoaders;
  private final Map<String, Object> options;
  private final List<DataFetcher<?>> fetchers;

  private List<ModelLoader<Model, ?>> filteredLoaders;

  public LoadDataSet(Model model, int width, int height, List<ModelLoader<Model, ?>> modelLoaders,
      Map<String, Object> options) {
    this.model = model;
    this.width = width;
    this.height = height;
    this.modelLoaders = modelLoaders;
    this.options = options;
    fetchers = new ArrayList<>(modelLoaders.size());
  }

  public boolean isEmpty() {
    return modelLoaders.isEmpty();
  }

  public void cancel() {
    for (DataFetcher<?> fetcher : fetchers) {
      if (fetcher != null) {
        fetcher.cancel();
      }
    }
  }

  @Override
  public Iterator<ModelLoader.LoadData<?>> iterator() {
    return new LoadDataIterator();
  }

  private synchronized List<ModelLoader<Model, ?>> getFilteredLoaders() {
    if (filteredLoaders == null) {
      filteredLoaders = new ArrayList<>(modelLoaders.size());
      for (ModelLoader<Model, ?> loader : modelLoaders) {
        if (loader.handles(model)) {
          filteredLoaders.add(loader);
        }
      }
    }
    return filteredLoaders;
  }

  @Override
  public String toString() {
    return "DataFetcherSet{" + "fetchers=" + Arrays
        .toString(fetchers.toArray(new DataFetcher[fetchers.size()])) + ", modelLoaders=" + Arrays
        .toString(modelLoaders.toArray(new ModelLoader[modelLoaders.size()])) + "}";
  }

  class LoadDataIterator implements Iterator<ModelLoader.LoadData<?>> {

    private int currentIndex;

    @Override
    public boolean hasNext() {
      return currentIndex < getFilteredLoaders().size();
    }

    @Override
    public ModelLoader.LoadData<?> next() {
      return getFilteredLoaders().get(currentIndex++).buildLoadData(model, width, height, options);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
