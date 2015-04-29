package com.bumptech.glide.load.engine;

import android.util.Log;

import com.bumptech.glide.Logs;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoader.LoadData;
import com.bumptech.glide.util.LogTime;

import java.util.Collections;
import java.util.List;

/**
 * Generates {@link com.bumptech.glide.load.data.DataFetcher DataFetchers} from original source data
 * using registered {@link com.bumptech.glide.load.model.ModelLoader ModelLoaders} and the model
 * provided for the load.
 *
 * <p> Depending on the disk cache strategy, source data may first be written to disk and then
 * loaded from the cache file rather than returned directly. </p>
 */
class SourceGenerator<Model> implements DataFetcherGenerator,
    DataFetcher.DataCallback<Object>,
    DataFetcherGenerator.FetcherReadyCallback {

  private final int width;
  private final int height;
  private final RequestContext<Model, ?> requestContext;
  private final DiskCache diskCache;
  private final FetcherReadyCallback cb;
  private final List<LoadData<?>> loadDataList;

  private int loadDataListIndex;
  private DataCacheGenerator sourceCacheGenerator;
  private Object dataToCache;
  private volatile ModelLoader.LoadData<?> loadData;

  public SourceGenerator(int width, int height, RequestContext<Model, ?> requestContext,
      DiskCache diskCache, FetcherReadyCallback cb) {
    this.width = width;
    this.height = height;
    this.requestContext = requestContext;
    this.diskCache = diskCache;
    this.cb = cb;

    loadDataList = requestContext.getLoadData();
  }

  @Override
  public boolean startNext() {
    if (dataToCache != null) {
      cacheData();
      dataToCache = null;
    }
    if (sourceCacheGenerator != null && sourceCacheGenerator.startNext()) {
      return true;
    }
    sourceCacheGenerator = null;

    loadData = null;
    while (loadData == null && hasNextModelLoader()) {
      loadData = loadDataList.get(loadDataListIndex++);
      if (loadData != null) {
        loadData.fetcher.loadData(requestContext.getPriority(), this);
      }
    }
    return loadData != null;
  }

  private boolean hasNextModelLoader() {
    return loadDataListIndex < loadDataList.size();
  }

  private void cacheData() {
    long startTime = LogTime.getLogTime();
    try {
      Encoder<Object> encoder = requestContext.getSourceEncoder(dataToCache);
      DataCacheWriter<Object> writer =
          new DataCacheWriter<>(encoder, dataToCache, requestContext.getOptions());
      Key originalKey = new DataCacheKey(loadData.sourceKey, requestContext.getSignature());
      diskCache.put(originalKey, writer);
      if (Logs.isEnabled(Log.VERBOSE)) {
        Logs.log(Log.VERBOSE, "Finished encoding source to cache"
            + ", key: " + originalKey
            + ", data: " + dataToCache
            + ", encoder: " + encoder
            + ", duration: " + LogTime.getElapsedMillis(startTime));
      }
    } finally {
      loadData.fetcher.cleanup();
    }

    sourceCacheGenerator =
        new DataCacheGenerator(Collections.singletonList(loadData.sourceKey), width, height,
            diskCache, requestContext, this);
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
    DiskCacheStrategy diskCacheStrategy = requestContext.getDiskCacheStrategy();
    if (data != null && diskCacheStrategy.isDataCacheable(loadData.fetcher.getDataSource())) {
      dataToCache = data;
      // We might be being called back on someone else's thread. Before doing anything, we should
      // reschedule to get back onto Glide's thread.
      cb.reschedule();
    } else {
      cb.onDataFetcherReady(loadData.sourceKey, data, loadData.fetcher,
          loadData.fetcher.getDataSource());
    }
  }

  @Override
  public void reschedule() {
    // We don't expect this to happen, although if we ever need it to we can delegate to our
    // callback.
    throw new UnsupportedOperationException();
  }

  // Called from source cache generator.
  @Override
  public void onDataFetcherReady(Key sourceKey, Object data, DataFetcher<?> fetcher,
      DataSource dataSource) {
    // This data fetcher will be loading from a File and provide the wrong data source, so override
    // with the data source of the original fetcher
    cb.onDataFetcherReady(sourceKey, data, fetcher, loadData.fetcher.getDataSource());
  }
}
