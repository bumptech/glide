package com.bumptech.glide.load.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pools.Pool;
import com.bumptech.glide.Registry.NoModelLoaderAvailableException;
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
// Hides Model throughout.
@SuppressWarnings("TypeParameterHidesVisibleType")
public class ModelLoaderRegistry {

  private final MultiModelLoaderFactory multiModelLoaderFactory;
  private final ModelLoaderCache cache = new ModelLoaderCache();

  public ModelLoaderRegistry(@NonNull Pool<List<Throwable>> throwableListPool) {
    this(new MultiModelLoaderFactory(throwableListPool));
  }

  private ModelLoaderRegistry(@NonNull MultiModelLoaderFactory multiModelLoaderFactory) {
    this.multiModelLoaderFactory = multiModelLoaderFactory;
  }

  public synchronized <Model, Data> void append(
      @NonNull Class<Model> modelClass,
      @NonNull Class<Data> dataClass,
      @NonNull ModelLoaderFactory<? extends Model, ? extends Data> factory) {
    multiModelLoaderFactory.append(modelClass, dataClass, factory);
    cache.clear();
  }

  public synchronized <Model, Data> void prepend(
      @NonNull Class<Model> modelClass,
      @NonNull Class<Data> dataClass,
      @NonNull ModelLoaderFactory<? extends Model, ? extends Data> factory) {
    multiModelLoaderFactory.prepend(modelClass, dataClass, factory);
    cache.clear();
  }

  public synchronized <Model, Data> void remove(
      @NonNull Class<Model> modelClass, @NonNull Class<Data> dataClass) {
    tearDown(multiModelLoaderFactory.remove(modelClass, dataClass));
    cache.clear();
  }

  public synchronized <Model, Data> void replace(
      @NonNull Class<Model> modelClass,
      @NonNull Class<Data> dataClass,
      @NonNull ModelLoaderFactory<? extends Model, ? extends Data> factory) {
    tearDown(multiModelLoaderFactory.replace(modelClass, dataClass, factory));
    cache.clear();
  }

  private <Model, Data> void tearDown(
      @NonNull List<ModelLoaderFactory<? extends Model, ? extends Data>> factories) {
    for (ModelLoaderFactory<? extends Model, ? extends Data> factory : factories) {
      factory.teardown();
    }
  }

  // We're allocating in a loop to avoid allocating empty lists that will never have anything added
  // to them.
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @NonNull
  public <A> List<ModelLoader<A, ?>> getModelLoaders(@NonNull A model) {
    List<ModelLoader<A, ?>> modelLoaders = getModelLoadersForClass(getClass(model));
    if (modelLoaders.isEmpty()) {
      throw new NoModelLoaderAvailableException(model);
    }
    int size = modelLoaders.size();
    boolean isEmpty = true;
    List<ModelLoader<A, ?>> filteredLoaders = Collections.emptyList();
    //noinspection ForLoopReplaceableByForEach to improve perf
    for (int i = 0; i < size; i++) {
      ModelLoader<A, ?> loader = modelLoaders.get(i);
      if (loader.handles(model)) {
        if (isEmpty) {
          filteredLoaders = new ArrayList<>(size - i);
          isEmpty = false;
        }
        filteredLoaders.add(loader);
      }
    }
    if (filteredLoaders.isEmpty()) {
      throw new NoModelLoaderAvailableException(model, modelLoaders);
    }
    return filteredLoaders;
  }

  public synchronized <Model, Data> ModelLoader<Model, Data> build(
      @NonNull Class<Model> modelClass, @NonNull Class<Data> dataClass) {
    return multiModelLoaderFactory.build(modelClass, dataClass);
  }

  @NonNull
  public synchronized List<Class<?>> getDataClasses(@NonNull Class<?> modelClass) {
    return multiModelLoaderFactory.getDataClasses(modelClass);
  }

  @NonNull
  private synchronized <A> List<ModelLoader<A, ?>> getModelLoadersForClass(
      @NonNull Class<A> modelClass) {
    List<ModelLoader<A, ?>> loaders = cache.get(modelClass);
    if (loaders == null) {
      loaders = Collections.unmodifiableList(multiModelLoaderFactory.build(modelClass));
      cache.put(modelClass, loaders);
    }
    return loaders;
  }

  @NonNull
  @SuppressWarnings("unchecked")
  private static <A> Class<A> getClass(@NonNull A model) {
    return (Class<A>) model.getClass();
  }

  private static class ModelLoaderCache {
    private final Map<Class<?>, Entry<?>> cachedModelLoaders = new HashMap<>();

    @Synthetic
    ModelLoaderCache() {}

    public void clear() {
      cachedModelLoaders.clear();
    }

    public <Model> void put(Class<Model> modelClass, List<ModelLoader<Model, ?>> loaders) {
      Entry<?> previous = cachedModelLoaders.put(modelClass, new Entry<>(loaders));
      if (previous != null) {
        throw new IllegalStateException("Already cached loaders for model: " + modelClass);
      }
    }

    @Nullable
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
