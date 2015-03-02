package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.model.ModelLoader;

import java.io.File;
import java.util.Iterator;
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
  private DataFetcher<?> fetcher;
  private Iterator<ModelLoader.LoadData<?>> loadDataIterator;

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
    while (loadDataIterator == null || !loadDataIterator.hasNext()) {
      sourceIdIndex++;
      if (sourceIdIndex >= sourceIds.size()) {
        return false;
      }

      Key sourceId = sourceIds.get(sourceIdIndex);
      Key originalKey = new DataCacheKey(sourceId, requestContext.getSignature());
      File cacheFile = diskCache.get(originalKey);
      if (cacheFile != null) {
        this.sourceKey = originalKey;
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
