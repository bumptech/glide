package com.bumptech.glide.load.engine;

import android.util.Log;

import com.bumptech.glide.Logs;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.util.LogTime;

import java.util.Collections;
import java.util.Iterator;

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
  private final Iterator<ModelLoader.LoadData<?>> dataLoaderIterator;

  private ModelLoader.LoadData<?> loadData;
  private DataCacheGenerator sourceCacheGenerator;

  public SourceGenerator(int width, int height,
      RequestContext<Model, ?> requestContext, DiskCache diskCache, FetcherReadyCallback cb) {
    this.width = width;
    this.height = height;
    this.requestContext = requestContext;
    this.diskCache = diskCache;
    this.cb = cb;

    dataLoaderIterator = requestContext.getLoadDataSet().iterator();
  }

  @Override
  public boolean startNext() {
    loadData = null;
    if (sourceCacheGenerator != null && sourceCacheGenerator.startNext()) {
      return true;
    }
    sourceCacheGenerator = null;

    while (loadData == null && dataLoaderIterator.hasNext()) {
      loadData = dataLoaderIterator.next();
      if (loadData != null) {
        loadData.fetcher.loadData(requestContext.getPriority(), this);
      }
    }
    return loadData != null;
  }

  @Override
  public void onDataReady(Object data) {
    DiskCacheStrategy diskCacheStrategy = requestContext.getDiskCacheStrategy();
    if (data != null && diskCacheStrategy.isDataCacheable(loadData.fetcher.getDataSource())) {
      long startTime = LogTime.getLogTime();
      try {
        Encoder<Object> encoder = requestContext.getSourceEncoder(data);
        DataCacheWriter<Object> writer =
            new DataCacheWriter<>(encoder, data, requestContext.getOptions());
        Key originalKey = new DataCacheKey(loadData.sourceKey, requestContext.getSignature());
        diskCache.put(originalKey, writer);
        if (Logs.isEnabled(Log.VERBOSE)) {
          Logs.log(Log.VERBOSE, "Finished encoding source to cache"
              + ", key: " + originalKey
              + ", data: " + data
              + ", encoder: " + encoder
              + ", duration: " + LogTime.getElapsedMillis(startTime));
        }
      } finally {
        loadData.fetcher.cleanup();
      }

      sourceCacheGenerator =
          new DataCacheGenerator(Collections.singletonList(loadData.sourceKey), width, height,
              diskCache, requestContext, this);
      if (!sourceCacheGenerator.startNext()) {
        cb.onDataFetcherReady(loadData.sourceKey, null /*data*/, loadData.fetcher,
            loadData.fetcher.getDataSource());
      }
    } else {
      cb.onDataFetcherReady(loadData.sourceKey, data, loadData.fetcher,
          loadData.fetcher.getDataSource());
    }
  }

  // Called from source cache generator.
  @Override
  public void onDataFetcherReady(Key sourceKey, Object data, DataFetcher fetcher,
      DataSource dataSource) {
    // This data fetcher will be loading from a File and provide the wrong data source, so override
    // with the data source of the original fetcher
    cb.onDataFetcherReady(sourceKey, data, fetcher, loadData.fetcher.getDataSource());
  }
}
