package com.bumptech.glide.load.engine;

import com.bumptech.glide.GlideContext;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.load.engine.DecodeJob.DiskCacheProvider;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoader.LoadData;
import com.bumptech.glide.load.resource.UnitTransformation;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class DecodeHelper<Transcode> {

  private final List<LoadData<?>> loadData = new ArrayList<>();
  private final List<Key> cacheKeys = new ArrayList<>();

  private GlideContext glideContext;
  private Object model;
  private int width;
  private int height;
  private Class<?> resourceClass;
  private DecodeJob.DiskCacheProvider diskCacheProvider;
  private Options options;
  private Map<Class<?>, Transformation<?>> transformations;
  private Class<Transcode> transcodeClass;
  private boolean isLoadDataSet;
  private boolean isCacheKeysSet;
  private Key signature;
  private Priority priority;
  private DiskCacheStrategy diskCacheStrategy;
  private boolean isTransformationRequired;
  private boolean isScaleOnlyOrNoTransform;

  @SuppressWarnings("unchecked")
  <R> void init(
      GlideContext glideContext,
      Object model,
      Key signature,
      int width,
      int height,
      DiskCacheStrategy diskCacheStrategy,
      Class<?> resourceClass,
      Class<R> transcodeClass,
      Priority priority,
      Options options,
      Map<Class<?>, Transformation<?>> transformations,
      boolean isTransformationRequired,
      boolean isScaleOnlyOrNoTransform,
      DiskCacheProvider diskCacheProvider) {
    this.glideContext = glideContext;
    this.model = model;
    this.signature = signature;
    this.width = width;
    this.height = height;
    this.diskCacheStrategy = diskCacheStrategy;
    this.resourceClass = resourceClass;
    this.diskCacheProvider = diskCacheProvider;
    this.transcodeClass = (Class<Transcode>) transcodeClass;
    this.priority = priority;
    this.options = options;
    this.transformations = transformations;
    this.isTransformationRequired = isTransformationRequired;
    this.isScaleOnlyOrNoTransform = isScaleOnlyOrNoTransform;
  }

  void clear() {
    glideContext = null;
    model = null;
    signature = null;
    resourceClass = null;
    transcodeClass = null;
    options = null;
    priority = null;
    transformations = null;
    diskCacheStrategy = null;

    loadData.clear();
    isLoadDataSet = false;
    cacheKeys.clear();
    isCacheKeysSet = false;
  }

  DiskCache getDiskCache() {
    return diskCacheProvider.getDiskCache();
  }

  DiskCacheStrategy getDiskCacheStrategy() {
    return diskCacheStrategy;
  }

  <T> DataRewinder<T> getRewinder(T data) {
    return glideContext.getRegistry().getRewinder(data);
  }

  Priority getPriority() {
    return priority;
  }

  Options getOptions() {
    return options;
  }

  Key getSignature() {
    return signature;
  }

  int getWidth() {
    return width;
  }

  int getHeight() {
    return height;
  }

  ArrayPool getArrayPool() {
    return glideContext.getArrayPool();
  }

  Class<?> getTranscodeClass() {
    return transcodeClass;
  }

  Class<?> getModelClass() {
    return model.getClass();
  }

  List<Class<?>> getRegisteredResourceClasses() {
    return glideContext
        .getRegistry()
        .getRegisteredResourceClasses(model.getClass(), resourceClass, transcodeClass);
  }

  boolean hasLoadPath(Class<?> dataClass) {
    return getLoadPath(dataClass) != null;
  }

  <Data> LoadPath<Data, ?, Transcode> getLoadPath(Class<Data> dataClass) {
    return glideContext.getRegistry().getLoadPath(dataClass, resourceClass, transcodeClass);
  }

  boolean isScaleOnlyOrNoTransform() {
    return isScaleOnlyOrNoTransform;
  }

  @SuppressWarnings("unchecked")
  <Z> Transformation<Z> getTransformation(Class<Z> resourceClass) {
    Transformation<Z> result = (Transformation<Z>) transformations.get(resourceClass);
    if (result == null) {
      for (Entry<Class<?>, Transformation<?>> entry : transformations.entrySet()) {
        if (entry.getKey().isAssignableFrom(resourceClass)) {
          result = (Transformation<Z>) entry.getValue();
          break;
        }
      }
    }

    if (result == null) {
      if (transformations.isEmpty() && isTransformationRequired) {
        throw new IllegalArgumentException(
            "Missing transformation for "
                + resourceClass
                + ". If you wish to"
                + " ignore unknown resource types, use the optional transformation methods.");
      } else {
        return UnitTransformation.get();
      }
    }
    return result;
  }

  boolean isResourceEncoderAvailable(Resource<?> resource) {
    return glideContext.getRegistry().isResourceEncoderAvailable(resource);
  }

  <Z> ResourceEncoder<Z> getResultEncoder(Resource<Z> resource) {
    return glideContext.getRegistry().getResultEncoder(resource);
  }

  List<ModelLoader<File, ?>> getModelLoaders(File file)
      throws Registry.NoModelLoaderAvailableException {
    return glideContext.getRegistry().getModelLoaders(file);
  }

  boolean isSourceKey(Key key) {
    List<LoadData<?>> loadData = getLoadData();
    //noinspection ForLoopReplaceableByForEach to improve perf
    for (int i = 0, size = loadData.size(); i < size; i++) {
      LoadData<?> current = loadData.get(i);
      if (current.sourceKey.equals(key)) {
        return true;
      }
    }
    return false;
  }

  List<LoadData<?>> getLoadData() {
    if (!isLoadDataSet) {
      isLoadDataSet = true;
      loadData.clear();
      List<ModelLoader<Object, ?>> modelLoaders = glideContext.getRegistry().getModelLoaders(model);
      //noinspection ForLoopReplaceableByForEach to improve perf
      for (int i = 0, size = modelLoaders.size(); i < size; i++) {
        ModelLoader<Object, ?> modelLoader = modelLoaders.get(i);
        LoadData<?> current = modelLoader.buildLoadData(model, width, height, options);
        if (current != null) {
          loadData.add(current);
        }
      }
    }
    return loadData;
  }

  List<Key> getCacheKeys() {
    if (!isCacheKeysSet) {
      isCacheKeysSet = true;
      cacheKeys.clear();
      List<LoadData<?>> loadData = getLoadData();
      //noinspection ForLoopReplaceableByForEach to improve perf
      for (int i = 0, size = loadData.size(); i < size; i++) {
        LoadData<?> data = loadData.get(i);
        if (!cacheKeys.contains(data.sourceKey)) {
          cacheKeys.add(data.sourceKey);
        }
        for (int j = 0; j < data.alternateKeys.size(); j++) {
          if (!cacheKeys.contains(data.alternateKeys.get(j))) {
            cacheKeys.add(data.alternateKeys.get(j));
          }
        }
      }
    }
    return cacheKeys;
  }

  <X> Encoder<X> getSourceEncoder(X data) throws Registry.NoSourceEncoderAvailableException {
    return glideContext.getRegistry().getSourceEncoder(data);
  }
}
