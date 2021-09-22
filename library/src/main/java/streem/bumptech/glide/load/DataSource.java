package com.bumptech.glide.load;

/** Indicates the origin of some retrieved data. */
public enum DataSource {
  /**
   * Indicates data was probably retrieved locally from the device, although it may have been
   * obtained through a content provider that may have obtained the data from a remote source.
   */
  LOCAL,
  /** Indicates data was retrieved from a remote source other than the device. */
  REMOTE,
  /** Indicates data was retrieved unmodified from the on device cache. */
  DATA_DISK_CACHE,
  /** Indicates data was retrieved from modified content in the on device cache. */
  RESOURCE_DISK_CACHE,
  /** Indicates data was retrieved from the in memory cache. */
  MEMORY_CACHE,
}
