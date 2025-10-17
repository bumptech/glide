package com.bumptech.glide.load.engine;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.DataFetcher.DataCallback;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoader.LoadData;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Synthetic;
import java.io.IOException;
import java.util.Collections;

/**
 * Generates {@link com.bumptech.glide.load.data.DataFetcher DataFetchers} from original source data
 * using registered {@link com.bumptech.glide.load.model.ModelLoader ModelLoaders} and the model
 * provided for the load.
 *
 * <p>Depending on the disk cache strategy, source data may first be written to disk and then loaded
 * from the cache file rather than returned directly.
 *
 * <p>This object may be used by multiple threads, but only one at a time. It is not safe to access
 * this object on multiple threads concurrently.
 */
class SourceGenerator implements DataFetcherGenerator, DataFetcherGenerator.FetcherReadyCallback {
  private static final String TAG = "SourceGenerator";

  private final DecodeHelper<?> helper;
  private final FetcherReadyCallback cb;

  private volatile int loadDataListIndex;
  private volatile DataCacheGenerator sourceCacheGenerator;
  private volatile Object dataToCache;
  private volatile ModelLoader.LoadData<?> loadData;
  private volatile DataCacheKey originalKey;

  SourceGenerator(DecodeHelper<?> helper, FetcherReadyCallback cb) {
    this.helper = helper;
    this.cb = cb;
  }

  // Concurrent access isn't supported.
  @SuppressWarnings({"NonAtomicOperationOnVolatileField", "NonAtomicVolatileUpdate"})
  @Override
  public boolean startNext() {
    if (dataToCache != null) {
      Object data = dataToCache;
      dataToCache = null;
      try {
        boolean isDataInCache = cacheData(data);
        // If we failed to write the data to cache, the cacheData method will try to decode the
        // original data directly instead of going through the disk cache. Since cacheData has
        // already called our callback at this point, there's nothing more to do but return.
        if (!isDataInCache) {
          return true;
        }
        // If we were able to write the data to cache successfully, we now need to proceed to call
        // the sourceCacheGenerator below to load the data from cache.
      } catch (IOException e) {
        // An IOException means we weren't able to write data to cache or we weren't able to rewind
        // it after a disk cache write failed. In either case we can just move on and try the next
        // fetch below.
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Failed to properly rewind or write data to cache", e);
        }
      }
    }

    if (sourceCacheGenerator != null && sourceCacheGenerator.startNext()) {
      return true;
    }
    sourceCacheGenerator = null;

    loadData = null;
    boolean started = false;
    while (!started && hasNextModelLoader()) {
      loadData = helper.getLoadData().get(loadDataListIndex++);
      if (loadData != null
          && (helper.getDiskCacheStrategy().isDataCacheable(loadData.fetcher.getDataSource())
              || helper.hasLoadPath(loadData.fetcher.getDataClass()))) {
        started = true;
        startNextLoad(loadData);
      }
    }
    return started;
  }

  private void startNextLoad(final LoadData<?> toStart) {
    loadData.fetcher.loadData(
        helper.getPriority(),
        new DataCallback<Object>() {
          @Override
          public void onDataReady(@Nullable Object data) {
            if (isCurrentRequest(toStart)) {
              onDataReadyInternal(toStart, data);
            }
          }

          @Override
          public void onLoadFailed(@NonNull Exception e) {
            if (isCurrentRequest(toStart)) {
              onLoadFailedInternal(toStart, e);
            }
          }
        });
  }

  // We want reference equality explicitly to make sure we ignore results from old requests.
  @SuppressWarnings({"PMD.CompareObjectsWithEquals", "WeakerAccess"})
  @Synthetic
  boolean isCurrentRequest(LoadData<?> requestLoadData) {
    LoadData<?> currentLoadData = loadData;
    return currentLoadData != null && currentLoadData == requestLoadData;
  }

  private boolean hasNextModelLoader() {
    return loadDataListIndex < helper.getLoadData().size();
  }

  /**
   * Returns {@code true} if we were able to cache the data and should try to decode the data
   * directly from cache and {@code false} if we were unable to cache the data and should make an
   * attempt to decode from source.
   */
  private boolean cacheData(Object dataToCache) throws IOException {
    long startTime = LogTime.getLogTime();
    boolean isLoadingFromSourceData = false;
    try {
      DataRewinder<Object> rewinder = helper.getRewinder(dataToCache);
      Object data = rewinder.rewindAndGet();
      Encoder<Object> encoder = helper.getSourceEncoder(data);
      DataCacheWriter<Object> writer = new DataCacheWriter<>(encoder, data, helper.getOptions());
      DataCacheKey newOriginalKey = new DataCacheKey(loadData.sourceKey, helper.getSignature());
      DiskCache diskCache = helper.getDiskCache();
      diskCache.put(newOriginalKey, writer);
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(
            TAG,
            "Finished encoding source to cache"
                + ", key: "
                + newOriginalKey
                + ", data: "
                + dataToCache
                + ", encoder: "
                + encoder
                + ", duration: "
                + LogTime.getElapsedMillis(startTime));
      }

      if (diskCache.get(newOriginalKey) != null) {
        originalKey = newOriginalKey;
        sourceCacheGenerator =
            new DataCacheGenerator(Collections.singletonList(loadData.sourceKey), helper, this);
        // We were able to write the data to cache.
        return true;
      } else {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(
              TAG,
              "Attempt to write: "
                  + originalKey
                  + ", data: "
                  + dataToCache
                  + " to the disk"
                  + " cache failed, maybe the disk cache is disabled?"
                  + " Trying to decode the data directly...");
        }

        isLoadingFromSourceData = true;
        cb.onDataFetcherReady(
            loadData.sourceKey,
            rewinder.rewindAndGet(),
            loadData.fetcher,
            loadData.fetcher.getDataSource(),
            loadData.sourceKey);
      }
      // We failed to write the data to cache.
      return false;
    } finally {
      if (!isLoadingFromSourceData) {
        loadData.fetcher.cleanup();
      }
    }
  }

  @Override
  public void cancel() {
    LoadData<?> local = loadData;
    if (local != null) {
      local.fetcher.cancel();
    }
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  void onDataReadyInternal(LoadData<?> loadData, Object data) {
    DiskCacheStrategy diskCacheStrategy = helper.getDiskCacheStrategy();
    if (data != null && diskCacheStrategy.isDataCacheable(loadData.fetcher.getDataSource())) {
      dataToCache = data;
      // We might be being called back on someone else's thread. Before doing anything, we should
      // reschedule to get back onto Glide's thread. Then once we're back on Glide's thread, we'll
      // get called again and we can write the retrieved data to cache.
      cb.reschedule();
    } else {
      cb.onDataFetcherReady(
          loadData.sourceKey,
          data,
          loadData.fetcher,
          loadData.fetcher.getDataSource(),
          originalKey);
    }
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  void onLoadFailedInternal(LoadData<?> loadData, @NonNull Exception e) {
    cb.onDataFetcherFailed(originalKey, e, loadData.fetcher, loadData.fetcher.getDataSource());
  }

  @Override
  public void reschedule() {
    // We don't expect this to happen, although if we ever need it to we can delegate to our
    // callback.
    throw new UnsupportedOperationException();
  }

  // Called from source cache generator.
  @Override
  public void onDataFetcherReady(
      Key sourceKey, Object data, DataFetcher<?> fetcher, DataSource dataSource, Key attemptedKey) {
    // This data fetcher will be loading from a File and provide the wrong data source, so override
    // with the data source of the original fetcher
    cb.onDataFetcherReady(sourceKey, data, fetcher, loadData.fetcher.getDataSource(), sourceKey);
  }

  @Override
  public void onDataFetcherFailed(
      Key sourceKey, Exception e, DataFetcher<?> fetcher, DataSource dataSource) {
    cb.onDataFetcherFailed(sourceKey, e, fetcher, loadData.fetcher.getDataSource());
  }
}
