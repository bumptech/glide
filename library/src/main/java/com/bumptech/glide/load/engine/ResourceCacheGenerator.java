package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.model.ModelLoader;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Generates {@link com.bumptech.glide.load.data.DataFetcher DataFetchers} from cache files
 * containing downsampled/transformed resource data.
 */
class ResourceCacheGenerator implements DataFetcherGenerator,
    DataFetcher.DataCallback<Object> {

  private final List<Key> sourceIds;
  private final List<Class<?>> resourceClasses;
  private final int width;
  private final int height;
  private final DiskCache diskCache;
  private final RequestContext<?, ?> requestContext;
  private final FetcherReadyCallback cb;

  private int sourceIdIndex = 0;
  private int resourceClassIndex = -1;
  private DataFetcher<?> fetcher;
  private Key sourceKey;
  private Iterator<ModelLoader.LoadData<?>> loadDataIterator;

  public ResourceCacheGenerator(List<Key> sourceIds,
      List<Class<?>> resourceClasses, int width, int height, DiskCache diskCache,
      RequestContext<?, ?> requestContext, FetcherReadyCallback cb) {
    this.sourceIds = sourceIds;
    this.resourceClasses = resourceClasses;
    this.width = width;
    this.height = height;
    this.diskCache = diskCache;
    this.requestContext = requestContext;
    this.cb = cb;
  }

  @Override
  public boolean startNext() {
    while (loadDataIterator == null || !loadDataIterator.hasNext()) {
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
      Transformation<?> transformation = requestContext.getTransformation(resourceClass);

      Key key = new ResourceCacheKey(sourceId, requestContext.getSignature(), width, height,
          transformation, resourceClass);
      File cacheFile = diskCache.get(key);
      if (cacheFile != null) {
        this.sourceKey = key;
        loadDataIterator = requestContext.getDataFetchers(cacheFile, width, height).iterator();
      }
    }

    fetcher = null;
    while (fetcher == null && loadDataIterator.hasNext()) {
      fetcher = loadDataIterator.next().fetcher;
      if (fetcher != null) {
        fetcher.loadData(requestContext.getPriority(), this);
      }
    }

    return fetcher != null;
  }

  @Override
  public void onDataReady(Object data) {
    cb.onDataFetcherReady(sourceKey, data, fetcher);
  }
}
