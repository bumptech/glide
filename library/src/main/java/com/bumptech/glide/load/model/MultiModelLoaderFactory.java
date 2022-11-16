package com.bumptech.glide.load.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Pools.Pool;
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
// Hides Model throughout.
@SuppressWarnings("TypeParameterHidesVisibleType")
public class MultiModelLoaderFactory {
  private static final Factory DEFAULT_FACTORY = new Factory();
  private static final ModelLoader<Object, Object> EMPTY_MODEL_LOADER = new EmptyModelLoader();
  private final List<Entry<?, ?>> entries = new ArrayList<>();
  private final Factory factory;
  private final Set<Entry<?, ?>> alreadyUsedEntries = new HashSet<>();
  private final Pool<List<Throwable>> throwableListPool;

  public MultiModelLoaderFactory(@NonNull Pool<List<Throwable>> throwableListPool) {
    this(throwableListPool, DEFAULT_FACTORY);
  }

  @VisibleForTesting
  MultiModelLoaderFactory(
      @NonNull Pool<List<Throwable>> throwableListPool, @NonNull Factory factory) {
    this.throwableListPool = throwableListPool;
    this.factory = factory;
  }

  synchronized <Model, Data> void append(
      @NonNull Class<Model> modelClass,
      @NonNull Class<Data> dataClass,
      @NonNull ModelLoaderFactory<? extends Model, ? extends Data> factory) {
    add(modelClass, dataClass, factory, /* append= */ true);
  }

  synchronized <Model, Data> void prepend(
      @NonNull Class<Model> modelClass,
      @NonNull Class<Data> dataClass,
      @NonNull ModelLoaderFactory<? extends Model, ? extends Data> factory) {
    add(modelClass, dataClass, factory, /* append= */ false);
  }

  private <Model, Data> void add(
      @NonNull Class<Model> modelClass,
      @NonNull Class<Data> dataClass,
      @NonNull ModelLoaderFactory<? extends Model, ? extends Data> factory,
      boolean append) {
    Entry<Model, Data> entry = new Entry<>(modelClass, dataClass, factory);
    entries.add(append ? entries.size() : 0, entry);
  }

  @NonNull
  synchronized <Model, Data> List<ModelLoaderFactory<? extends Model, ? extends Data>> replace(
      @NonNull Class<Model> modelClass,
      @NonNull Class<Data> dataClass,
      @NonNull ModelLoaderFactory<? extends Model, ? extends Data> factory) {
    List<ModelLoaderFactory<? extends Model, ? extends Data>> removed =
        remove(modelClass, dataClass);
    append(modelClass, dataClass, factory);
    return removed;
  }

  @NonNull
  synchronized <Model, Data> List<ModelLoaderFactory<? extends Model, ? extends Data>> remove(
      @NonNull Class<Model> modelClass, @NonNull Class<Data> dataClass) {
    List<ModelLoaderFactory<? extends Model, ? extends Data>> factories = new ArrayList<>();
    for (Iterator<Entry<?, ?>> iterator = entries.iterator(); iterator.hasNext(); ) {
      Entry<?, ?> entry = iterator.next();
      if (entry.handles(modelClass, dataClass)) {
        iterator.remove();
        factories.add(this.<Model, Data>getFactory(entry));
      }
    }
    return factories;
  }

  @NonNull
  synchronized <Model> List<ModelLoader<Model, ?>> build(@NonNull Class<Model> modelClass) {
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

  @NonNull
  synchronized List<Class<?>> getDataClasses(@NonNull Class<?> modelClass) {
    List<Class<?>> result = new ArrayList<>();
    for (Entry<?, ?> entry : entries) {
      if (!result.contains(entry.dataClass) && entry.handles(modelClass)) {
        result.add(entry.dataClass);
      }
    }
    return result;
  }

  @NonNull
  public synchronized <Model, Data> ModelLoader<Model, Data> build(
      @NonNull Class<Model> modelClass, @NonNull Class<Data> dataClass) {
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
        return factory.build(loaders, throwableListPool);
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

  @NonNull
  @SuppressWarnings("unchecked")
  private <Model, Data> ModelLoaderFactory<Model, Data> getFactory(@NonNull Entry<?, ?> entry) {
    return (ModelLoaderFactory<Model, Data>) entry.factory;
  }

  @NonNull
  @SuppressWarnings("unchecked")
  private <Model, Data> ModelLoader<Model, Data> build(@NonNull Entry<?, ?> entry) {
    return (ModelLoader<Model, Data>) Preconditions.checkNotNull(entry.factory.build(this));
  }

  @NonNull
  @SuppressWarnings("unchecked")
  private static <Model, Data> ModelLoader<Model, Data> emptyModelLoader() {
    return (ModelLoader<Model, Data>) EMPTY_MODEL_LOADER;
  }

  private static class Entry<Model, Data> {
    private final Class<Model> modelClass;
    @Synthetic final Class<Data> dataClass;
    @Synthetic final ModelLoaderFactory<? extends Model, ? extends Data> factory;

    public Entry(
        @NonNull Class<Model> modelClass,
        @NonNull Class<Data> dataClass,
        @NonNull ModelLoaderFactory<? extends Model, ? extends Data> factory) {
      this.modelClass = modelClass;
      this.dataClass = dataClass;
      this.factory = factory;
    }

    public boolean handles(@NonNull Class<?> modelClass, @NonNull Class<?> dataClass) {
      return handles(modelClass) && this.dataClass.isAssignableFrom(dataClass);
    }

    public boolean handles(@NonNull Class<?> modelClass) {
      return this.modelClass.isAssignableFrom(modelClass);
    }
  }

  static class Factory {
    @NonNull
    public <Model, Data> MultiModelLoader<Model, Data> build(
        @NonNull List<ModelLoader<Model, Data>> modelLoaders,
        @NonNull Pool<List<Throwable>> throwableListPool) {
      return new MultiModelLoader<>(modelLoaders, throwableListPool);
    }
  }

  private static class EmptyModelLoader implements ModelLoader<Object, Object> {
    @Synthetic
    EmptyModelLoader() {}

    @Nullable
    @Override
    public LoadData<Object> buildLoadData(
        @NonNull Object o, int width, int height, @NonNull Options options) {
      return null;
    }

    @Override
    public boolean handles(@NonNull Object o) {
      return false;
    }
  }
}
