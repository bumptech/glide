package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.EncodeStrategy;

/**
 * Set of available caching strategies for media.
 */
public enum DiskCacheStrategy {

  /**
   * Caches remote data with both {@link #DATA} and {@link #RESOURCE}, and local data with
   * {@link #RESOURCE} only.
   */
  ALL(true /*decodeCachedData*/, true /*decodeCachedResource*/) {
    @Override
    public boolean isDataCacheable(DataSource dataSource) {
      return dataSource == DataSource.REMOTE;
    }

    @Override
    public boolean isResourceCacheable(boolean isFromAlternateCacheKey, DataSource dataSource,
        EncodeStrategy encodeStrategy) {
      return dataSource != DataSource.RESOURCE_DISK_CACHE && dataSource != DataSource.MEMORY_CACHE;
    }
  },
  /**
   * Saves no data to cache.
   */
  NONE(false /*decodeCachedData*/, false /*decodeCachedResource*/) {
    @Override
    public boolean isDataCacheable(DataSource dataSource) {
      return false;
    }

    @Override
    public boolean isResourceCacheable(boolean isFromAlternateCacheKey, DataSource dataSource,
        EncodeStrategy encodeStrategy) {
      return false;
    }
  },
  /**
   * Writes retrieved data directly to the disk cache before it's decoded.
   */
  DATA(true /*decodeCachedData*/, false /*decodeCachedResource*/) {
    @Override
    public boolean isDataCacheable(DataSource dataSource) {
      return dataSource != DataSource.DATA_DISK_CACHE && dataSource != DataSource.MEMORY_CACHE;
    }

    @Override
    public boolean isResourceCacheable(boolean isFromAlternateCacheKey, DataSource dataSource,
        EncodeStrategy encodeStrategy) {
      return false;
    }
  },
  /**
   * Writes resources to disk after they've been decoded.
   */
  RESOURCE(false /*decodeCachedData*/, true /*decodeCachedResource*/) {
    @Override
    public boolean isDataCacheable(DataSource dataSource) {
      return false;
    }

    @Override
    public boolean isResourceCacheable(boolean isFromAlternateCacheKey, DataSource dataSource,
        EncodeStrategy encodeStrategy) {
      return dataSource != DataSource.RESOURCE_DISK_CACHE && dataSource != DataSource.MEMORY_CACHE;
    }
  },
  /**
   * Tries to intelligently choose a strategy based on the data source of the
   * {@link com.bumptech.glide.load.data.DataFetcher} and the
   * {@link com.bumptech.glide.load.EncodeStrategy} of the
   * {@link com.bumptech.glide.load.ResourceEncoder} (if an
   * {@link com.bumptech.glide.load.ResourceEncoder} is available).
   */
  AUTOMATIC(true /*decodeCachedData*/, true /*decodeCachedResource*/) {
    @Override
    public boolean isDataCacheable(DataSource dataSource) {
      return dataSource == DataSource.REMOTE;
    }

    @Override
    public boolean isResourceCacheable(boolean isFromAlternateCacheKey, DataSource dataSource,
        EncodeStrategy encodeStrategy) {
      return ((isFromAlternateCacheKey && dataSource == DataSource.DATA_DISK_CACHE)
          || dataSource == DataSource.LOCAL)
          && encodeStrategy == EncodeStrategy.TRANSFORMED;
    }
  };

  private final boolean decodeCachedData;
  private final boolean decodeCachedResource;

  DiskCacheStrategy(boolean decodeCachedData,
      boolean decodeCachedResource) {
    this.decodeCachedData = decodeCachedData;
    this.decodeCachedResource = decodeCachedResource;
  }

  /**
   * Returns true if this request should cache the original unmodified data.
   */
  public abstract boolean isDataCacheable(DataSource dataSource);

  /**
   * Returns true if this request should cache the final transformed resource.
   */
  public abstract boolean isResourceCacheable(boolean isFromAlternateCacheKey,
      DataSource dataSource, EncodeStrategy encodeStrategy);

  /**
   * Returns true if this request should attempt to decode cached resource data.
   */
  public boolean decodeCachedResource() {
    return decodeCachedResource;
  }

  /**
   * Returns true if this request should attempt to decode cached source data.
   */
  public boolean decodeCachedData() {
    return decodeCachedData;
  }
}
