package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.EncodeStrategy;

/** Set of available caching strategies for media. */
public abstract class DiskCacheStrategy {

  /**
   * Caches remote data with both {@link #DATA} and {@link #RESOURCE}, and local data with {@link
   * #RESOURCE} only.
   */
  public static final DiskCacheStrategy ALL =
      new DiskCacheStrategy() {
        @Override
        public boolean isDataCacheable(DataSource dataSource) {
          return dataSource == DataSource.REMOTE;
        }

        @Override
        public boolean isResourceCacheable(
            boolean isFromAlternateCacheKey, DataSource dataSource, EncodeStrategy encodeStrategy) {
          return dataSource != DataSource.RESOURCE_DISK_CACHE
              && dataSource != DataSource.MEMORY_CACHE;
        }

        @Override
        public boolean decodeCachedResource() {
          return true;
        }

        @Override
        public boolean decodeCachedData() {
          return true;
        }
      };

  /** Saves no data to cache. */
  public static final DiskCacheStrategy NONE =
      new DiskCacheStrategy() {
        @Override
        public boolean isDataCacheable(DataSource dataSource) {
          return false;
        }

        @Override
        public boolean isResourceCacheable(
            boolean isFromAlternateCacheKey, DataSource dataSource, EncodeStrategy encodeStrategy) {
          return false;
        }

        @Override
        public boolean decodeCachedResource() {
          return false;
        }

        @Override
        public boolean decodeCachedData() {
          return false;
        }
      };

  /** Writes retrieved data directly to the disk cache before it's decoded. */
  public static final DiskCacheStrategy DATA =
      new DiskCacheStrategy() {
        @Override
        public boolean isDataCacheable(DataSource dataSource) {
          return dataSource != DataSource.DATA_DISK_CACHE && dataSource != DataSource.MEMORY_CACHE;
        }

        @Override
        public boolean isResourceCacheable(
            boolean isFromAlternateCacheKey, DataSource dataSource, EncodeStrategy encodeStrategy) {
          return false;
        }

        @Override
        public boolean decodeCachedResource() {
          return false;
        }

        @Override
        public boolean decodeCachedData() {
          return true;
        }
      };

  /** Writes resources to disk after they've been decoded. */
  public static final DiskCacheStrategy RESOURCE =
      new DiskCacheStrategy() {
        @Override
        public boolean isDataCacheable(DataSource dataSource) {
          return false;
        }

        @Override
        public boolean isResourceCacheable(
            boolean isFromAlternateCacheKey, DataSource dataSource, EncodeStrategy encodeStrategy) {
          return dataSource != DataSource.RESOURCE_DISK_CACHE
              && dataSource != DataSource.MEMORY_CACHE;
        }

        @Override
        public boolean decodeCachedResource() {
          return true;
        }

        @Override
        public boolean decodeCachedData() {
          return false;
        }
      };

  /**
   * Tries to intelligently choose a strategy based on the data source of the {@link
   * com.bumptech.glide.load.data.DataFetcher} and the {@link
   * com.bumptech.glide.load.EncodeStrategy} of the {@link com.bumptech.glide.load.ResourceEncoder}
   * (if an {@link com.bumptech.glide.load.ResourceEncoder} is available).
   */
  public static final DiskCacheStrategy AUTOMATIC =
      new DiskCacheStrategy() {
        @Override
        public boolean isDataCacheable(DataSource dataSource) {
          return dataSource == DataSource.REMOTE;
        }

        @Override
        public boolean isResourceCacheable(
            boolean isFromAlternateCacheKey, DataSource dataSource, EncodeStrategy encodeStrategy) {
          return ((isFromAlternateCacheKey && dataSource == DataSource.DATA_DISK_CACHE)
                  || dataSource == DataSource.LOCAL)
              && encodeStrategy == EncodeStrategy.TRANSFORMED;
        }

        @Override
        public boolean decodeCachedResource() {
          return true;
        }

        @Override
        public boolean decodeCachedData() {
          return true;
        }
      };

  /**
   * Returns true if this request should cache the original unmodified data.
   *
   * @param dataSource Indicates where the data was originally retrieved.
   */
  public abstract boolean isDataCacheable(DataSource dataSource);

  /**
   * Returns true if this request should cache the final transformed resource.
   *
   * @param isFromAlternateCacheKey {@code true} if the resource we've decoded was loaded using an
   *     alternative, rather than the primary, cache key.
   * @param dataSource Indicates where the data used to decode the resource was originally
   *     retrieved.
   * @param encodeStrategy The {@link EncodeStrategy} the {@link
   *     com.bumptech.glide.load.ResourceEncoder} will use to encode the resource.
   */
  public abstract boolean isResourceCacheable(
      boolean isFromAlternateCacheKey, DataSource dataSource, EncodeStrategy encodeStrategy);

  /** Returns true if this request should attempt to decode cached resource data. */
  public abstract boolean decodeCachedResource();

  /** Returns true if this request should attempt to decode cached source data. */
  public abstract boolean decodeCachedData();
}
