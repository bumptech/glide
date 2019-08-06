package com.bumptech.glide;

/** Defines how to respond to {@code onTrimMemory} events sent by the platform. */
public enum TrimMemoryStrategy {
  /**
   * Clear memory held in caches if {@code onLowMemory} is called.
   *
   * <p>This may happen in either the foreground or background.
   */
  DEFAULT,

  /**
   * In addition to the default behavior, clear memory used by all in-progress and loaded requests
   * if {@code onTrimMemory} is called with level = {@code TRIM_MEMORY_MODERATE}.
   *
   * <p>It is expected this level is never sent if the app is in the foreground.
   */
  PAUSE_ALL_ON_TRIM_MODERATE,
}
