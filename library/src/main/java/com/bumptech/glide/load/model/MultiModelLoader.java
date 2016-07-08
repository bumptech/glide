package com.bumptech.glide.load.model;

import android.support.v4.util.Pools.Pool;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.DataFetcher.DataCallback;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.util.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Allows attempting multiple ModelLoaders registered for a given model and data class.
 *
 * <p> TODO: we should try to find a way to remove this class. It exists to allow individual
 * ModelLoaders to delegate to multiple ModelLoaders without having to duplicate this logic
 * everywhere. We have very similar logic in the {@link
 * com.bumptech.glide.load.engine.DataFetcherGenerator} implementations and should try to avoid this
 * duplication. </p>
 */
class MultiModelLoader<Model, Data> implements ModelLoader<Model, Data> {

  private final List<ModelLoader<Model, Data>> modelLoaders;
  private final Pool<List<Exception>> exceptionListPool;

  MultiModelLoader(List<ModelLoader<Model, Data>> modelLoaders,
      Pool<List<Exception>> exceptionListPool) {
    this.modelLoaders = modelLoaders;
    this.exceptionListPool = exceptionListPool;
  }

  @Override
  public LoadData<Data> buildLoadData(Model model, int width, int height,
      Options options) {
    Key sourceKey = null;
    int size = modelLoaders.size();
    List<DataFetcher<Data>> fetchers = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      ModelLoader<Model, Data> modelLoader = modelLoaders.get(i);
      if (modelLoader.handles(model)) {
        LoadData<Data> loadData = modelLoader.buildLoadData(model, width, height, options);
        if (loadData != null) {
          sourceKey = loadData.sourceKey;
          fetchers.add(loadData.fetcher);
        }
      }
    }
    return !fetchers.isEmpty()
        ? new LoadData<>(sourceKey, new MultiFetcher<>(fetchers, exceptionListPool)) : null;
  }

  @Override
  public boolean handles(Model model) {
    for (ModelLoader<Model, Data> modelLoader : modelLoaders) {
      if (modelLoader.handles(model)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "MultiModelLoader{" + "modelLoaders=" + Arrays
        .toString(modelLoaders.toArray(new ModelLoader[modelLoaders.size()])) + '}';
  }

  static class MultiFetcher<Data> implements DataFetcher<Data>, DataCallback<Data> {

    private final List<DataFetcher<Data>> fetchers;
    private final Pool<List<Exception>> exceptionListPool;
    private int currentIndex;
    private Priority priority;
    private DataCallback<? super Data> callback;
    private List<Exception> exceptions;

    MultiFetcher(List<DataFetcher<Data>> fetchers, Pool<List<Exception>> exceptionListPool) {
      this.exceptionListPool = exceptionListPool;
      Preconditions.checkNotEmpty(fetchers);
      this.fetchers = fetchers;
      currentIndex = 0;
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super Data> callback) {
      this.priority = priority;
      this.callback = callback;
      exceptions = exceptionListPool.acquire();
      fetchers.get(currentIndex).loadData(priority, this);
    }

    @Override
    public void cleanup() {
      exceptionListPool.release(exceptions);
      exceptions = null;
      for (DataFetcher<Data> fetcher : fetchers) {
        fetcher.cleanup();
      }
    }

    @Override
    public void cancel() {
      for (DataFetcher<Data> fetcher : fetchers) {
        fetcher.cancel();
      }
    }

    @Override
    public Class<Data> getDataClass() {
      return fetchers.get(0).getDataClass();
    }

    @Override
    public DataSource getDataSource() {
      return fetchers.get(0).getDataSource();
    }

    @Override
    public void onDataReady(Data data) {
      if (data != null) {
        callback.onDataReady(data);
      } else {
        startNextOrFail();
      }
    }

    @Override
    public void onLoadFailed(Exception e) {
      exceptions.add(e);
      startNextOrFail();
    }

    private void startNextOrFail() {
      if (currentIndex < fetchers.size() - 1) {
        currentIndex++;
        loadData(priority, callback);
      } else {
        callback.onLoadFailed(new GlideException("Fetch failed", new ArrayList<>(exceptions)));
      }
    }
  }
}
