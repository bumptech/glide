package com.bumptech.glide.load.engine.bitmap_recycle;

/**
 * Pool containing byte[] arrays of various sizes.
 */
public interface ByteArrayPool {
  // 16KB
  int DEFAULT_BUFFER = 64 * 1024;

  /**
   * Optionally adds the given byte array to the pool.
   *
   * <p> Arrays may be ignored, for example if the array is larger than the maximum size of the
   * pool </p>
   */
  void put(byte[] bytes);

  /**
   * Returns a non-null byte array with a length >= to the given size.
   *
   * <p> If an array of the given size isn't in the pool, a new one will be allocated. </p>
   */
  byte[] get(int size);

  /**
   * Clears all byte arrays from the pool.
   */
  void clearMemory();

  /**
   * Trims the size to the appropriate level.
   *
   * @param level A trim specified in {@link android.content.ComponentCallbacks2}.
   */
  void trimMemory(int level);
}
