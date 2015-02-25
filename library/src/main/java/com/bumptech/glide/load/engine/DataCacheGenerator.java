package com.bumptech.glide.load.engine;

import android.util.Log;

import com.bumptech.glide.Logs;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Generates {@link com.bumptech.glide.load.data.DataFetcher DataFetchers} from cache files
 * containing original unmodified source data.
 */
class DataCacheGenerator implements DataFetcherGenerator,
    DataFetcher.DataCallback<Object> {

  private final List<String> sourceIds;
  private final int width;
  private final int height;
  private final DiskCache diskCache;
  private final RequestContext<?> requestContext;
  private final FetcherReadyCallback cb;

  private int sourceIdIndex = -1;
  private DataFetcher<?> fetcher;
  private Iterator<DataFetcher<?>> cacheFetchers;

  public DataCacheGenerator(List<String> sourceIds, int width, int height, DiskCache diskCache,
      RequestContext requestContext, FetcherReadyCallback cb) {
    this.sourceIds = sourceIds;
    this.width = width;
    this.height = height;
    this.diskCache = diskCache;
    this.requestContext = requestContext;
    this.cb = cb;
  }

  @Override
  public boolean startNext() {
    while (cacheFetchers == null || !cacheFetchers.hasNext()) {
      sourceIdIndex++;
      if (sourceIdIndex >= sourceIds.size()) {
        return false;
      }

      String sourceId = sourceIds.get(sourceIdIndex);
      Key originalKey = new DataCacheKey(sourceId, requestContext.getSignature());
      File cacheFile = diskCache.get(originalKey);
      if (cacheFile != null) {
        cacheFetchers = requestContext.getDataFetchers(cacheFile, width, height).iterator();
      }
    }

    fetcher = null;
    while (fetcher == null && cacheFetchers.hasNext()) {
      fetcher = cacheFetchers.next();
      if (fetcher == null) {
        continue;
      }
      try {
        fetcher.loadData(requestContext.getPriority(), this);
      } catch (IOException e) {
        if (Logs.isEnabled(Log.DEBUG)) {
          Logs.log(Log.DEBUG, "Failed to load data from data cache fetcher", e);
        }
        fetcher = null;
      }
    }
    return fetcher != null;
  }

  @Override
  public void onDataReady(Object data) {
    cb.onDataFetcherReady(sourceIds.get(sourceIdIndex), data, fetcher);
  }
}
