package com.bumptech.glide.load.model;

import android.content.Context;

import com.bumptech.glide.Registry.NoModelLoaderAvailableException;
import com.bumptech.glide.util.Preconditions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Capable of building an {@link ModelLoader} that wraps one or more other {@link ModelLoader}s for
 * a given model and data class.
 */
public class MultiModelLoaderFactory {
  private static final Factory DEFAULT_FACTORY = new Factory();
  private final List<Entry<?, ?>> entries = new ArrayList<>();
  private final Context context;
  private final Factory factory;

  public MultiModelLoaderFactory(Context context) {
    this(context, DEFAULT_FACTORY);
  }

  MultiModelLoaderFactory(Context context, Factory factory) {
    this.context = context.getApplicationContext();
    this.factory = factory;
  }

  <Model, Data> void append(Class<Model> modelClass, Class<Data> dataClass,
      ModelLoaderFactory<Model, Data> factory) {
    add(modelClass, dataClass, factory, true /*append*/);
  }

  <Model, Data> void prepend(Class<Model> modelClass, Class<Data> dataClass,
      ModelLoaderFactory<Model, Data> factory) {
    add(modelClass, dataClass, factory, false /*append*/);
  }

  private <Model, Data> void add(Class<Model> modelClass, Class<Data> dataClass,
      ModelLoaderFactory<Model, Data> factory, boolean append) {
    Entry<Model, Data> entry = new Entry<>(modelClass, dataClass, factory);
    entries.add(append ? entries.size() : 0, entry);
  }

  <Model, Data> List<ModelLoaderFactory<Model, Data>> replace(Class<Model> modelClass,
      Class<Data> dataClass, ModelLoaderFactory<Model, Data> factory) {
    List<ModelLoaderFactory<Model, Data>> removed = remove(modelClass, dataClass);
    append(modelClass, dataClass, factory);
    return removed;
  }

  <Model, Data> List<ModelLoaderFactory<Model, Data>> remove(Class<Model> modelClass,
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

  <Model> List<ModelLoader<Model, ?>> build(Class<Model> modelClass) {
    List<ModelLoader<Model, ?>> loaders = new ArrayList<>();
    for (Entry<?, ?> entry : entries) {
      if (entry.handles(modelClass)) {
        loaders.add(this.<Model, Object>build(entry));
      }
    }
    return loaders;
  }

  List<Class<?>> getDataClasses(Class<?> modelClass) {
    List<Class<?>> result = new ArrayList<>();
    for (Entry<?, ?> entry : entries) {
      if (!result.contains(entry.dataClass) && entry.handles(modelClass)) {
        result.add(entry.dataClass);
      }
    }
    return result;
  }

  public <Model, Data> ModelLoader<Model, Data> build(Class<Model> modelClass,
      Class<Data> dataClass) {
    List<ModelLoader<Model, Data>> loaders = new ArrayList<>();
    for (Entry<?, ?> entry : entries) {
      if (entry.handles(modelClass, dataClass)) {
        loaders.add(this.<Model, Data>build(entry));
      }
    }
    if (loaders.size() > 1) {
      return factory.build(loaders);
    } else if (loaders.size() == 1) {
      return loaders.get(0);
    } else {
      throw new NoModelLoaderAvailableException(modelClass, dataClass);
    }
  }

  @SuppressWarnings("unchecked")
  private <Model, Data> ModelLoaderFactory<Model, Data> getFactory(Entry<?, ?> entry) {
    return (ModelLoaderFactory<Model, Data>) entry.factory;
  }

  @SuppressWarnings("unchecked")
  private <Model, Data> ModelLoader<Model, Data> build(Entry<?, ?> entry) {
    return (ModelLoader<Model, Data>) Preconditions
        .checkNotNull(entry.factory.build(context, this));
  }

  private static class Entry<Model, Data> {
    private final Class<Model> modelClass;
    private final Class<Data> dataClass;
    private final ModelLoaderFactory<Model, Data> factory;

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
        List<ModelLoader<Model, Data>> modelLoaders) {
      return new MultiModelLoader<>(modelLoaders);
    }
  }
}
