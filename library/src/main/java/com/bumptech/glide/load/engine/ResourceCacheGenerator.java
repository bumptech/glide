package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoader.LoadData;
import java.io.File;
import java.util.List;

/**
 * Generates {@link com.bumptech.glide.load.data.DataFetcher DataFetchers} from cache files
 * containing downsampled/transformed resource data.
 */
class ResourceCacheGenerator implements DataFetcherGenerator,
    DataFetcher.DataCallback<Object> {

  private final FetcherReadyCallback cb;
  private final DecodeHelper<?> helper;

  private int sourceIdIndex = 0;
  private int resourceClassIndex = -1;
  private Key sourceKey;
  private List<ModelLoader<File, ?>> modelLoaders;
  private int modelLoaderIndex;
  private volatile LoadData<?> loadData;
  // PMD is wrong here, this File must be an instance variable because it may be used across
  // multiple calls to startNext.
  @SuppressWarnings("PMD.SingularField")
  private File cacheFile;
  private ResourceCacheKey currentKey;

  public ResourceCacheGenerator(DecodeHelper<?> helper, FetcherReadyCallback cb) {
    this.helper = helper;
    this.cb = cb;
  }

  @Override
  public boolean startNext() {
    List<Key> sourceIds = helper.getCacheKeys();
    if (sourceIds.isEmpty()) {
      return false;
    }
    List<Class<?>> resourceClasses = helper.getRegisteredResourceClasses();
    while (modelLoaders == null || !hasNextModelLoader()) {
      resourceClassIndex++;
      if (resourceClassIndex >= resourceClasses.size()) {
        sourceIdIndex++;
        if (sourceIdIndex >= sourceIds.size()) {
          return false;
        }
        resourceClassIndex = 0;
      }

      Key sourceId = sourceIds.get(sourceIdIndex);
      Class<?> resourceClass = resourceClasses.get(resourceClassIndex);
      Transformation<?> transformation = helper.getTransformation(resourceClass);

      currentKey = new ResourceCacheKey(sourceId, helper.getSignature(), helper.getWidth(),
          helper.getHeight(), transformation, resourceClass, helper.getOptions());
      cacheFile = helper.getDiskCache().get(currentKey);
      if (cacheFile != null) {
        this.sourceKey = sourceId;
        modelLoaders = helper.getModelLoaders(cacheFile);
        modelLoaderIndex = 0;
      }
    }

    loadData = null;
    boolean started = false;
    while (!started && hasNextModelLoader()) {
      ModelLoader<File, ?> modelLoader = modelLoaders.get(modelLoaderIndex++);
      loadData =
          modelLoader.buildLoadData(cacheFile, helper.getWidth(), helper.getHeight(),
              helper.getOptions());
      if (loadData != null && helper.hasLoadPath(loadData.fetcher.getDataClass())) {
        started = true;
        loadData.fetcher.loadData(helper.getPriority(), this);
      }
    }

    return started;
  }

  private boolean hasNextModelLoader() {
    return modelLoaderIndex < modelLoaders.size();
  }

  @Override
  public void cancel() {
    LoadData<?> local = loadData;
    if (local != null) {
      local.fetcher.cancel();
    }
  }

  @Override
  public void onDataReady(Object data) {
    cb.onDataFetcherReady(sourceKey, data, loadData.fetcher, DataSource.RESOURCE_DISK_CACHE,
        currentKey);
  }

  @Override
  public void onLoadFailed(Exception e) {
    cb.onDataFetcherFailed(currentKey, e, loadData.fetcher, DataSource.RESOURCE_DISK_CACHE);
  }
}
