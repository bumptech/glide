package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;

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
class SourceGenerator implements DataFetcherGenerator,
    DataFetcher.DataCallback<Object> {

  private final Iterator<DataFetcher<?>> sourceFetchers;
  private final int width;
  private final int height;
  private final RequestContext<?> requestContext;
  private final DiskCache diskCache;
  private final FetcherReadyCallback cb;

  private DataFetcher<?> fetcher;
  private DataCacheGenerator sourceCacheGenerator;

  public SourceGenerator(int width, int height, RequestContext<?> requestContext,
      DiskCache diskCache, FetcherReadyCallback cb) {
    this.width = width;
    this.height = height;
    this.requestContext = requestContext;
    this.diskCache = diskCache;
    this.cb = cb;
    this.sourceFetchers = requestContext.getDataFetchers().iterator();
  }

  @Override
  public boolean startNext() {
    fetcher = null;
    if (sourceCacheGenerator != null && sourceCacheGenerator.startNext()) {
      return true;
    }
    sourceCacheGenerator = null;

    while (fetcher == null && sourceFetchers.hasNext()) {
      fetcher = sourceFetchers.next();
      if (fetcher != null) {
        fetcher.loadData(requestContext.getPriority(), this);
      }
    }
    return fetcher != null;
  }

  @Override
  public void onDataReady(Object data) {
    if (data != null
        && requestContext.getDiskCacheStrategy().cacheSource(fetcher.getDataSource())) {
      try {
        Encoder<Object> encoder = requestContext.getSourceEncoder(data);
        DataCacheWriter<Object> writer =
            new DataCacheWriter<>(encoder, data, requestContext.getOptions());
        Key originalKey = new DataCacheKey(fetcher.getId(), requestContext.getSignature());
        diskCache.put(originalKey, writer);
      } finally {
        fetcher.cleanup();
      }

      sourceCacheGenerator =
          new DataCacheGenerator(Collections.singletonList(fetcher.getId()), width, height,
              diskCache, requestContext, cb);
      if (!sourceCacheGenerator.startNext()) {
        cb.onDataFetcherReady(fetcher.getId(), null /*data*/, fetcher);
      }
    } else {
      cb.onDataFetcherReady(fetcher.getId(), data, fetcher);
    }
  }
}
