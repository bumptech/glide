package com.bumptech.glide.load;

import com.bumptech.glide.util.Preconditions;

import java.security.MessageDigest;

/**
 * A base class with reasonable defaults for applying arbitrary component related options.
 *
 * <p>
 *   Implementations must either be unique (usually declared as static final variables), or
 *   implement {@link #equals(Object)} and {@link #hashCode()}.
 * </p>
 *
 * <p>
 *   Implementations can implement {@link #update(Object, MessageDigest)} to make sure that
 *   the disk cache key includes the specific option set.
 * </p>
 *
 * @param <T> The type of the option ({@link Integer}, {@link
 * android.graphics.Bitmap.CompressFormat} etc.).
 */
public final class Option<T> implements Comparable<Option<?>> {

  private static final CacheKeyUpdater<Object> EMPTY_UPDATER = new CacheKeyUpdater<Object>() {
    @Override
    public void update(Object value, MessageDigest messageDigest) {
      // Do nothing.
    }
  };

  private final T defaultValue;
  private final CacheKeyUpdater<T> cacheKeyUpdater;
  private final String key;

  /**
   * Returns a new {@link Option} that does not affect disk cache keys with a {@code null} default
   * value.
   *
   * @param key A unique package prefixed {@link String} that identifies this option (must be
   *            stable across builds, so {@link Class#getName()} should <em>not</em> be used).
   */
  public static <T> Option<T> memory(String key) {
    return new Option<>(key, null /*defaultValue*/, Option.<T>emptyUpdater());
  }

  /**
   * Returns a new {@link Option} that does not affect disk cache keys with the given value as the
   * default value.
   *
   * @param key A unique package prefixed {@link String} that identifies this option (must be
   *            stable across builds, so {@link Class#getName()} should <em>not</em> be used).
   */
  public static <T> Option<T> memory(String key, T defaultValue) {
    return new Option<>(key, defaultValue, Option.<T>emptyUpdater());
  }

  /**
   * Returns a new {@link Option} that uses the given {@link
   * com.bumptech.glide.load.Option.CacheKeyUpdater} to update disk cache keys.
   *
   * @param key A unique package prefixed {@link String} that identifies this option (must be
   *            stable across builds, so {@link Class#getName()} should <em>not</em> be used).
   */
  public static <T> Option<T> disk(String key, CacheKeyUpdater<T> cacheKeyUpdater) {
    return new Option<>(key, null /*defaultValue*/, cacheKeyUpdater);
  }

  /**
   * Returns a new {@link Option} that uses the given {@link
   * com.bumptech.glide.load.Option.CacheKeyUpdater} to update disk cache keys and provides
   * the given value as the default value.
   *
   * @param key A unique package prefixed {@link String} that identifies this option (must be
   *            stable across builds, so {@link Class#getName()} should <em>not</em> be used).
   */
  public static <T> Option<T> disk(String key, T defaultValue, CacheKeyUpdater<T> cacheKeyUpdater) {
    return new Option<>(key, defaultValue, cacheKeyUpdater);
  }

  Option(String key, T defaultValue, CacheKeyUpdater<T> cacheKeyUpdater) {
    this.key = Preconditions.checkNotNull(key);
    this.defaultValue = defaultValue;
    this.cacheKeyUpdater = Preconditions.checkNotNull(cacheKeyUpdater);
  }

  public T getDefaultValue() {
    return defaultValue;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Option) {
      Option<?> other = (Option<?>) o;
      return key.equals(other.key);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public int compareTo(Option<?> another) {
    return key.compareTo(another.key);
  }

  public void update(T value, MessageDigest messageDigest) {
    cacheKeyUpdater.update(value, messageDigest);
  }

  @SuppressWarnings("unchecked")
  private static <T> CacheKeyUpdater<T> emptyUpdater() {
    return (CacheKeyUpdater<T>) EMPTY_UPDATER;
  }

  /**
   * An interface that updates a {@link MessageDigest} with the given value as part of a process to
   * generate a disk cache key.
   */
  public interface CacheKeyUpdater<T> {
    void update(T value, MessageDigest messageDigest);
  }
}
