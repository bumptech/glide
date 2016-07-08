package com.bumptech.glide.load;

import android.support.annotation.Nullable;
import com.bumptech.glide.util.Preconditions;
import java.security.MessageDigest;

/**
 * Defines available component (decoders, encoders, model loaders etc.) options with optional
 * default values and the ability to affect the resource disk cache key used by {@link
 * com.bumptech.glide.load.engine.DiskCacheStrategy#RESOURCE}.
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
 * android.graphics.Bitmap.CompressFormat} etc.), must implement {@link #equals(Object)} and
 * {@link #hashCode()}.
 */
public final class Option<T> {
  private static final CacheKeyUpdater<Object> EMPTY_UPDATER = new CacheKeyUpdater<Object>() {
    @Override
    public void update(byte[] keyBytes, Object value, MessageDigest messageDigest) {
      // Do nothing.
    }
  };

  private final T defaultValue;
  private final CacheKeyUpdater<T> cacheKeyUpdater;
  private final String key;
  private volatile byte[] keyBytes;

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
    this.key = Preconditions.checkNotEmpty(key);
    this.defaultValue = defaultValue;
    this.cacheKeyUpdater = Preconditions.checkNotNull(cacheKeyUpdater);
  }

  /**
   * Returns a reasonable default to use if no other value is set, or {@code null}.
   */
  @Nullable
  public T getDefaultValue() {
    return defaultValue;
  }

  /**
   * Updates the given {@link MessageDigest} used to construct a cache key with the given
   * value using the {@link com.bumptech.glide.load.Option.CacheKeyUpdater} optionally provided in
   * the constructor.
   */
  public void update(T value, MessageDigest messageDigest) {
    cacheKeyUpdater.update(getKeyBytes(), value, messageDigest);
  }

  private byte[] getKeyBytes() {
    if (keyBytes == null) {
      keyBytes = key.getBytes(Key.CHARSET);
    }
    return keyBytes;
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

  @SuppressWarnings("unchecked")
  private static <T> CacheKeyUpdater<T> emptyUpdater() {
    return (CacheKeyUpdater<T>) EMPTY_UPDATER;
  }

  @Override
  public String toString() {
    return "Option{"
        + "key='" + key + '\''
        + '}';
  }

  /**
   * An interface that updates a {@link MessageDigest} with the given value as part of a process to
   * generate a disk cache key.
   *
   * @param <T> The type of the option.
   */
  public interface CacheKeyUpdater<T> {
    /**
     * Updates the given {@link MessageDigest} with the bytes of the given key (to avoid incidental
     * value collisions when values are not particularly unique) and value.
     */
    void update(byte[] keyBytes, T value, MessageDigest messageDigest);
  }
}
