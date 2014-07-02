package com.bumptech.glide.load.engine;

/**
 * Set of available caching strategies for media
 */
public enum DiskCacheStrategy {
    /** Caches with both {@link #SOURCE} and {@link #RESULT} */
    ALL(true, true),
    /** Saves no data to cache */
    NONE(false, false),
    /** Saves just the original data to cache */
    SOURCE(true, false),
    /** Saves the media item after all transformations to cache */
    RESULT(false, true);

    private final boolean cacheSource;
    private final boolean cacheResult;

    DiskCacheStrategy(boolean cacheSource, boolean cacheResult) {
        this.cacheSource = cacheSource;
        this.cacheResult = cacheResult;
    }

    public boolean cacheSource() {
        return cacheSource;
    }

    public boolean cacheResult() {
        return cacheResult;
    }
}
