package com.bumptech.glide.load.data;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
  private final Options options;

  private List<ModelLoader<Model, ?>> filteredLoaders;

  public LoadDataSet(Model model, int width, int height, List<ModelLoader<Model, ?>> modelLoaders,
      Options options) {
    this.model = model;
    this.width = width;
    this.height = height;
    this.modelLoaders = modelLoaders;
    this.options = options;
  }

  public boolean isEmpty() {
    return modelLoaders.isEmpty();
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
    return "DataFetcherSet{modelLoaders=" +
        Arrays.toString(modelLoaders.toArray(new ModelLoader[modelLoaders.size()])) + "}";
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
