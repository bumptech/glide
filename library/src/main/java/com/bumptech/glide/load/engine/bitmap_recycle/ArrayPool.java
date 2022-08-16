package com.bumptech.glide.load.engine.bitmap_recycle;

/** Interface for an array pool that pools arrays of different types. */
public interface ArrayPool {
  /**
   * A standard size to use to increase hit rates when the required size isn't defined. Currently
   * 64KB.
   */
  int STANDARD_BUFFER_SIZE_BYTES = 64 * 1024;

  /**
   * Optionally adds the given array of the given type to the pool.
   *
   * <p>Arrays may be ignored, for example if the array is larger than the maximum size of the pool.
   *
   * @deprecated Use {@link #put(Object)}
   */
  @Deprecated
  <T> void put(T array, Class<T> arrayClass);

  /**
   * Optionally adds the given array of the given type to the pool.
   *
   * <p>Arrays may be ignored, for example if the array is larger than the maximum size of the pool.
   */
  <T> void put(T array);

  /**
   * Returns a non-null array of the given type with a length {@code >=} to the given size.
   *
   * <p>If an array of the given size isn't in the pool, a new one will be allocated.
   *
   * <p>This class makes no guarantees about the contents of the returned array.
   *
   * @see #getExact(int, Class)
   */
  <T> T get(int size, Class<T> arrayClass);

  /**
   * Returns a non-null array of the given type with a length exactly equal to the given size.
   *
   * <p>If an array of the given size isn't in the pool, a new one will be allocated.
   *
   * <p>This class makes no guarantees about the contents of the returned array.
   *
   * @see #get(int, Class)
   */
  <T> T getExact(int size, Class<T> arrayClass);

  /** Clears all arrays from the pool. */
  void clearMemory();

  /**
   * Trims the size to the appropriate level.
   *
   * @param level A trim specified in {@link android.content.ComponentCallbacks2}.
   */
  void trimMemory(int level);
}
