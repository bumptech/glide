package com.bumptech.glide.load.model;

import android.support.v4.util.Pools.Pool;
import com.bumptech.glide.util.Synthetic;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains an ordered put of {@link ModelLoader}s and the model and data types they handle in
 * order from highest priority to lowest.
 */
public class ModelLoaderRegistry {

  private final MultiModelLoaderFactory multiModelLoaderFactory;
  private final ModelLoaderCache cache = new ModelLoaderCache();

  public ModelLoaderRegistry(Pool<List<Exception>> exceptionListPool) {
    this(new MultiModelLoaderFactory(exceptionListPool));
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

  public synchronized <A> List<ModelLoader<A, ?>> getModelLoaders(A model) {
    List<ModelLoader<A, ?>> modelLoaders = getModelLoadersForClass(getClass(model));
    int size = modelLoaders.size();
    List<ModelLoader<A, ?>> filteredLoaders = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      ModelLoader<A, ?> loader = modelLoaders.get(i);
      if (loader.handles(model)) {
        filteredLoaders.add(loader);
      }
    }
    return filteredLoaders;
  }

  public synchronized <Model, Data> ModelLoader<Model, Data> build(Class<Model> modelClass,
      Class<Data> dataClass) {
    return multiModelLoaderFactory.build(modelClass, dataClass);
  }

  public synchronized List<Class<?>> getDataClasses(Class<?> modelClass) {
    return multiModelLoaderFactory.getDataClasses(modelClass);
  }

  private <A> List<ModelLoader<A, ?>> getModelLoadersForClass(Class<A> modelClass) {
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
    private final Map<Class<?>, Entry<?>> cachedModelLoaders = new HashMap<>();

    @Synthetic
    ModelLoaderCache() { }

    public void clear() {
      cachedModelLoaders.clear();
    }

    public <Model> void put(Class<Model> modelClass, List<ModelLoader<Model, ?>> loaders) {
      Entry<?> previous = cachedModelLoaders.put(modelClass, new Entry<>(loaders));
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
      @Synthetic final List<ModelLoader<Model, ?>> loaders;

      public Entry(List<ModelLoader<Model, ?>> loaders) {
        this.loaders = loaders;
      }
    }
  }
}
