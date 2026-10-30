package com.bumptech.glide.load.engine.cache;

import com.bumptech.glide.load.engine.BitmapInfo;

/** Metadata of a cached bitmap entry in the Memory Cache. */
public final class MemoryCacheEntryInfo {
  public final BitmapInfo bitmapInfo;
  public final long lastAccessedTimestamp;

  public MemoryCacheEntryInfo(BitmapInfo bitmapInfo, long lastAccessedTimestamp) {
    this.bitmapInfo = bitmapInfo;
    this.lastAccessedTimestamp = lastAccessedTimestamp;
  }
}
