package com.bumptech.glide.load.model;

import android.support.annotation.Nullable;
import android.support.v4.util.Pools.Pool;
import com.bumptech.glide.Registry.NoModelLoaderAvailableException;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Capable of building an {@link ModelLoader} that wraps one or more other {@link ModelLoader}s for
 * a given model and data class.
 */
public class MultiModelLoaderFactory {
  private static final Factory DEFAULT_FACTORY = new Factory();
  private static final ModelLoader<Object, Object> EMPTY_MODEL_LOADER = new EmptyModelLoader();
  private final List<Entry<?, ?>> entries = new ArrayList<>();
  private final Factory factory;
  private final Set<Entry<?, ?>> alreadyUsedEntries = new HashSet<>();
  private final Pool<List<Exception>> exceptionListPool;

  public MultiModelLoaderFactory(Pool<List<Exception>> exceptionListPool) {
    this(exceptionListPool, DEFAULT_FACTORY);
  }

  // Visible for testing.
  MultiModelLoaderFactory(Pool<List<Exception>> exceptionListPool,
      Factory factory) {
    this.exceptionListPool = exceptionListPool;
    this.factory = factory;
  }

  synchronized <Model, Data> void append(Class<Model> modelClass, Class<Data> dataClass,
      ModelLoaderFactory<Model, Data> factory) {
    add(modelClass, dataClass, factory, true /*append*/);
  }

  synchronized <Model, Data> void prepend(Class<Model> modelClass, Class<Data> dataClass,
      ModelLoaderFactory<Model, Data> factory) {
    add(modelClass, dataClass, factory, false /*append*/);
  }

  private <Model, Data> void add(Class<Model> modelClass, Class<Data> dataClass,
      ModelLoaderFactory<Model, Data> factory, boolean append) {
    Entry<Model, Data> entry = new Entry<>(modelClass, dataClass, factory);
    entries.add(append ? entries.size() : 0, entry);
  }

  synchronized <Model, Data> List<ModelLoaderFactory<Model, Data>> replace(Class<Model> modelClass,
      Class<Data> dataClass, ModelLoaderFactory<Model, Data> factory) {
    List<ModelLoaderFactory<Model, Data>> removed = remove(modelClass, dataClass);
    append(modelClass, dataClass, factory);
    return removed;
  }

  synchronized <Model, Data> List<ModelLoaderFactory<Model, Data>> remove(Class<Model> modelClass,
      Class<Data> dataClass) {
    List<ModelLoaderFactory<Model, Data>> factories = new ArrayList<>();
    for (Iterator<Entry<?, ?>> iterator = entries.iterator(); iterator.hasNext(); ) {
      Entry<?, ?> entry = iterator.next();
      if (entry.handles(modelClass, dataClass)) {
        iterator.remove();
        factories.add(this.<Model, Data>getFactory(entry));
      }
    }
    return factories;
  }

  synchronized <Model> List<ModelLoader<Model, ?>> build(Class<Model> modelClass) {
    try {
      List<ModelLoader<Model, ?>> loaders = new ArrayList<>();
      for (Entry<?, ?> entry : entries) {
        // Avoid stack overflow recursively creating model loaders by only creating loaders in
        // recursive requests if they haven't been created earlier in the chain. For example:
        // A Uri loader may translate to another model, which in turn may translate back to a Uri.
        // The original Uri loader won't be provided to the intermediate model loader, although
        // other Uri loaders will be.
        if (alreadyUsedEntries.contains(entry)) {
          continue;
        }
        if (entry.handles(modelClass)) {
          alreadyUsedEntries.add(entry);
          loaders.add(this.<Model, Object>build(entry));
          alreadyUsedEntries.remove(entry);
        }
      }
      return loaders;
    } catch (Throwable t) {
      alreadyUsedEntries.clear();
      throw t;
    }
  }

  synchronized List<Class<?>> getDataClasses(Class<?> modelClass) {
    List<Class<?>> result = new ArrayList<>();
    for (Entry<?, ?> entry : entries) {
      if (!result.contains(entry.dataClass) && entry.handles(modelClass)) {
        result.add(entry.dataClass);
      }
    }
    return result;
  }

  public synchronized <Model, Data> ModelLoader<Model, Data> build(Class<Model> modelClass,
      Class<Data> dataClass) {
    try {
      List<ModelLoader<Model, Data>> loaders = new ArrayList<>();
      boolean ignoredAnyEntries = false;
      for (Entry<?, ?> entry : entries) {
        // Avoid stack overflow recursively creating model loaders by only creating loaders in
        // recursive requests if they haven't been created earlier in the chain. For example:
        // A Uri loader may translate to another model, which in turn may translate back to a Uri.
        // The original Uri loader won't be provided to the intermediate model loader, although
        // other Uri loaders will be.
        if (alreadyUsedEntries.contains(entry)) {
          ignoredAnyEntries = true;
          continue;
        }
        if (entry.handles(modelClass, dataClass)) {
          alreadyUsedEntries.add(entry);
          loaders.add(this.<Model, Data>build(entry));
          alreadyUsedEntries.remove(entry);
        }
      }
      if (loaders.size() > 1) {
        return factory.build(loaders, exceptionListPool);
      } else if (loaders.size() == 1) {
        return loaders.get(0);
      } else {
        // Avoid crashing if recursion results in no loaders available. The assertion is supposed to
        // catch completely unhandled types, recursion may mean a subtype isn't handled somewhere
        // down the stack, which is often ok.
        if (ignoredAnyEntries) {
          return emptyModelLoader();
        } else {
          throw new NoModelLoaderAvailableException(modelClass, dataClass);
        }
      }
    } catch (Throwable t) {
      alreadyUsedEntries.clear();
      throw t;
    }
  }

  @SuppressWarnings("unchecked")
  private <Model, Data> ModelLoaderFactory<Model, Data> getFactory(Entry<?, ?> entry) {
    return (ModelLoaderFactory<Model, Data>) entry.factory;
  }

  @SuppressWarnings("unchecked")
  private <Model, Data> ModelLoader<Model, Data> build(Entry<?, ?> entry) {
    return (ModelLoader<Model, Data>) Preconditions.checkNotNull(entry.factory.build(this));
  }

  @SuppressWarnings("unchecked")
  private static <Model, Data> ModelLoader<Model, Data> emptyModelLoader() {
    return (ModelLoader<Model, Data>) EMPTY_MODEL_LOADER;
  }

  private static class Entry<Model, Data> {
    private final Class<Model> modelClass;
    @Synthetic final Class<Data> dataClass;
    @Synthetic final ModelLoaderFactory<Model, Data> factory;

    public Entry(Class<Model> modelClass, Class<Data> dataClass,
        ModelLoaderFactory<Model, Data> factory) {
      this.modelClass = modelClass;
      this.dataClass = dataClass;
      this.factory = factory;
    }

    public boolean handles(Class<?> modelClass, Class<?> dataClass) {
      return handles(modelClass) && this.dataClass.isAssignableFrom(dataClass);
    }

    public boolean handles(Class<?> modelClass) {
      return this.modelClass.isAssignableFrom(modelClass);
    }
  }

  static class Factory {
    public <Model, Data> MultiModelLoader<Model, Data> build(
        List<ModelLoader<Model, Data>> modelLoaders, Pool<List<Exception>> exceptionListPool) {
      return new MultiModelLoader<>(modelLoaders, exceptionListPool);
    }
  }

  private static class EmptyModelLoader implements ModelLoader<Object, Object> {

    @Synthetic
    EmptyModelLoader() { }

    @Nullable
    @Override
    public LoadData<Object> buildLoadData(Object o, int width, int height, Options options) {
      return null;
    }

    @Override
    public boolean handles(Object o) {
      return false;
    }
  }
}
