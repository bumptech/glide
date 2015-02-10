package com.bumptech.glide.load.engine;

/**
 * Set of available caching strategies for media.
 */
public enum DiskCacheStrategy {
    /** Caches with both {@link #DATA} and {@link #RESOURCE}. */
    ALL(true, true),
    /** Saves no data to cache. */
    NONE(false, false),
    /** Writes retrieved data directly to the disk cache before it's decoded. */
    DATA(true, false),
    /** Writes resources to disk after they've been decoded. */
    RESOURCE(false, true);

    private final boolean cacheData;
    private final boolean cacheResource;

    DiskCacheStrategy(boolean cacheData, boolean cacheResource) {
        this.cacheData = cacheData;
        this.cacheResource = cacheResource;
    }

    /**
     * Returns true if this request should cache the original unmodified data.
     */
    public boolean cacheSource() {
        return cacheData;
    }

    /**
     * Returns true if this request should cache the final transformed result.
     */
    public boolean cacheResult() {
        return cacheResource;
    }
}
