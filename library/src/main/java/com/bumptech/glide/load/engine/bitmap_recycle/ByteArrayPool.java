package com.bumptech.glide.load.engine.bitmap_recycle;

/**
 * Pool containing byte[] arrays of various sizes.
 * TODO: this should be replaced with an ArrayPool
 */
public interface ByteArrayPool {
  /**
   * A standard size to use to increase hit rates when the required size isn't defined.
   * Currently 64KB.
   */
  int STANDARD_BUFFER_SIZE_BYTES = 64 * 1024;

  /**
   * Optionally adds the given byte array to the pool.
   *
   * <p>Arrays may be ignored, for example if the array is larger than the maximum size of the
   * pool.
   */
  void put(byte[] bytes);

  /**
   * Returns a non-null byte array with a length >= to the given size.
   *
   * <p>If an array of the given size isn't in the pool, a new one will be allocated.
   */
  byte[] get(int size);
}
