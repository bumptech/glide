package com.bumptech.glide.load.engine.cache;

import java.io.File;

/**
 * Interface called out of UI thread to get the cache folder.
 */
public interface CacheDirectoryGetter {
  File getCacheDirectory();
}
