package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.model.ModelLoader;

import java.io.File;
import java.util.List;

/**
 * Generates {@link com.bumptech.glide.load.data.DataFetcher DataFetchers} from cache files
 * containing original unmodified source data.
 */
class DataCacheGenerator implements DataFetcherGenerator,
    DataFetcher.DataCallback<Object> {

  private final List<Key> sourceIds;
  private final int width;
  private final int height;
  private final DiskCache diskCache;
  private final RequestContext<?, ?> requestContext;
  private final FetcherReadyCallback cb;

  private int sourceIdIndex = -1;
  private Key sourceKey;
  private List<ModelLoader<File, ?>> modelLoaders;
  private int modelLoaderIndex;
  private volatile DataFetcher<?> fetcher;
  // PMD is wrong here, this File must be an instance variable because it may be used across
  // multiple calls to startNext.
  @SuppressWarnings("PMD.SingularField")
  private File cacheFile;

  public DataCacheGenerator(List<Key> sourceIds, int width, int height, DiskCache diskCache,
      RequestContext<?, ?> requestContext, FetcherReadyCallback cb) {
    this.sourceIds = sourceIds;
    this.width = width;
    this.height = height;
    this.diskCache = diskCache;
    this.requestContext = requestContext;
    this.cb = cb;
  }

  @Override
  public boolean startNext() {
    while (modelLoaders == null || !hasNextModelLoader()) {
      sourceIdIndex++;
      if (sourceIdIndex >= sourceIds.size()) {
        return false;
      }

      Key sourceId = sourceIds.get(sourceIdIndex);
      Key originalKey = new DataCacheKey(sourceId, requestContext.getSignature());
      cacheFile = diskCache.get(originalKey);
      if (cacheFile != null) {
        this.sourceKey = sourceId;
        modelLoaders = requestContext.getModelLoaders(cacheFile);
        modelLoaderIndex = 0;
      }
    }

    fetcher = null;
    while (fetcher == null && hasNextModelLoader()) {
      ModelLoader<File, ?> modelLoader = modelLoaders.get(modelLoaderIndex++);
      fetcher =
          modelLoader.buildLoadData(cacheFile, width, height, requestContext.getOptions()).fetcher;
      if (fetcher != null) {
        fetcher.loadData(requestContext.getPriority(), this);
      }
    }
    return fetcher != null;
  }

  private boolean hasNextModelLoader() {
    return modelLoaderIndex < modelLoaders.size();
  }

  @Override
  public void cancel() {
    DataFetcher<?> local = fetcher;
    if (local != null) {
      local.cancel();
    }
  }

  @Override
  public void onDataReady(Object data) {
    cb.onDataFetcherReady(sourceKey, data, fetcher, DataSource.DATA_DISK_CACHE);
  }
}
