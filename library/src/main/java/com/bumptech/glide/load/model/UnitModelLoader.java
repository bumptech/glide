package com.bumptech.glide.load.model;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.signature.ObjectKey;

/**
 * A put of helper classes that performs no loading and instead always returns the given model as
 * the data to decode.
 *
 * @param <Model> The type of model that will also be returned as decodable data.
 */
public class UnitModelLoader<Model> implements ModelLoader<Model, Model> {

  @Override
  public LoadData<Model> buildLoadData(Model model, int width, int height,
      Options options) {
    return new LoadData<>(new ObjectKey(model), new UnitFetcher<>(model));
  }

  @Override
  public boolean handles(Model model) {
    return true;
  }

  private static class UnitFetcher<Model> implements DataFetcher<Model> {

    private final Model resource;

    public UnitFetcher(Model resource) {
      this.resource = resource;
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super Model> callback) {
      callback.onDataReady(resource);
    }

    @Override
    public void cleanup() {
      // Do nothing.
    }

    @Override
    public void cancel() {
      // Do nothing.
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Model> getDataClass() {
      return (Class<Model>) resource.getClass();
    }

    @Override
    public DataSource getDataSource() {
      return DataSource.LOCAL;
    }
  }

  /**
   * Factory for producing {@link com.bumptech.glide.load.model.UnitModelLoader}s.
   *
   * @param <Model> The type of model that will also be returned as decodable data.
   */
  public static class Factory<Model> implements ModelLoaderFactory<Model, Model> {

    @Override
    public ModelLoader<Model, Model> build(MultiModelLoaderFactory multiFactory) {
      return new UnitModelLoader<>();
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }
}
