package com.bumptech.glide;

/** An enum for dynamically modifying the amount of memory Glide is able to use. */
public enum MemoryCategory {
  /**
   * Tells Glide's memory cache and bitmap pool to use at most half of their initial maximum size.
   */
  LOW(0.5f),
  /** Tells Glide's memory cache and bitmap pool to use at most their initial maximum size. */
  NORMAL(1f),
  /**
   * Tells Glide's memory cache and bitmap pool to use at most one and a half times their initial
   * maximum size.
   */
  HIGH(1.5f);

  private final float multiplier;

  MemoryCategory(float multiplier) {
    this.multiplier = multiplier;
  }

  /**
   * Returns the multiplier that should be applied to the initial maximum size of Glide's memory
   * cache and bitmap pool.
   */
  public float getMultiplier() {
    return multiplier;
  }
}
