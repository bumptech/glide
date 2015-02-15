package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.EncodeStrategy;

/**
 * Set of available caching strategies for media.
 */
public enum DiskCacheStrategy {

  /**
   * Caches with both {@link #DATA} and {@link #RESOURCE}.
   */
  ALL(new DiskCacheChooser() {
    @Override
    public boolean isSourceCachable(DataSource dataSource) {
      return true;
    }

    @Override
    public boolean isResourceCachable(DataSource dataSource, EncodeStrategy encodeStrategy) {
      return dataSource != DataSource.RESOURCE_DISK_CACHE && dataSource != DataSource.MEMORY_CACHE;
    }
  }, true /*decodeCachedData*/, true /*decodeCachedResource*/),
  /**
   * Saves no data to cache.
   */
  NONE(new DiskCacheChooser() {
    @Override
    public boolean isSourceCachable(DataSource dataSource) {
      return false;
    }

    @Override
    public boolean isResourceCachable(DataSource dataSource, EncodeStrategy encodeStrategy) {
      return false;
    }
  }, false /*decodeCachedData*/, false /*decodeCachedResource*/),
  /**
   * Writes retrieved data directly to the disk cache before it's decoded.
   */
  DATA(new DiskCacheChooser() {
    @Override
    public boolean isSourceCachable(DataSource dataSource) {
      return true;
    }

    @Override
    public boolean isResourceCachable(DataSource dataSource, EncodeStrategy encodeStrategy) {
      return false;
    }
  }, true /*decodeCachedData*/, false /*decodeCachedResource*/),
  /**
   * Writes resources to disk after they've been decoded.
   */
  RESOURCE(new DiskCacheChooser() {
    @Override
    public boolean isSourceCachable(DataSource dataSource) {
      return false;
    }

    @Override
    public boolean isResourceCachable(DataSource dataSource, EncodeStrategy encodeStrategy) {
      return dataSource != DataSource.RESOURCE_DISK_CACHE && dataSource != DataSource.MEMORY_CACHE;
    }
  }, false /*decodeCachedData*/, true /*decodeCachedResource*/),
  /**
   * Tries to intelligently choose a strategy based on the data source of the
   * {@link com.bumptech.glide.load.data.DataFetcher} and the
   * {@link com.bumptech.glide.load.EncodeStrategy} of the
   * {@link com.bumptech.glide.load.ResourceEncoder} (if an
   * {@link com.bumptech.glide.load.ResourceEncoder} is available).
   */
  AUTOMATIC(new DiskCacheChooser() {
    @Override
    public boolean isSourceCachable(DataSource dataSource) {
      return dataSource == DataSource.REMOTE;
    }

    @Override
    public boolean isResourceCachable(DataSource dataSource, EncodeStrategy encodeStrategy) {
      return dataSource == DataSource.LOCAL && encodeStrategy == EncodeStrategy.TRANSFORMED;
    }
  }, true /*decodeCachedData*/, true /*decodeCachedResource*/);

  private final DiskCacheChooser chooser;
  private final boolean decodeCachedData;
  private final boolean decodeCachedResource;

  private interface DiskCacheChooser {
    boolean isSourceCachable(DataSource dataSource);

    boolean isResourceCachable(DataSource dataSource, EncodeStrategy encodeStrategy);
  }

  DiskCacheStrategy(DiskCacheChooser chooser, boolean decodeCachedData,
      boolean decodeCachedResource) {
    this.chooser = chooser;
    this.decodeCachedData = decodeCachedData;
    this.decodeCachedResource = decodeCachedResource;
  }

  /**
   * Returns true if this request should cache the original unmodified data.
   */
  public boolean cacheSource(DataSource source) {
    return chooser.isSourceCachable(source);
  }

  /**
   * Returns true if this request should cache the final transformed result.
   */
  public boolean cacheResult(DataSource source, EncodeStrategy encodeStrategy) {
    return chooser.isResourceCachable(source, encodeStrategy);
  }

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
