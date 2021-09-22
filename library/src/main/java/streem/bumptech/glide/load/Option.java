package com.bumptech.glide.load;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.util.Preconditions;
import java.security.MessageDigest;

/**
 * Defines available component (decoders, encoders, model loaders etc.) options with optional
 * default values and the ability to affect the resource disk cache key used by {@link
 * com.bumptech.glide.load.engine.DiskCacheStrategy#RESOURCE}.
 *
 * <p>Implementations must either be unique (usually declared as static final variables), or
 * implement {@link #equals(Object)} and {@link #hashCode()}.
 *
 * <p>Implementations can implement {@link #update(Object, MessageDigest)} to make sure that the
 * disk cache key includes the specific option set.
 *
 * @param <T> The type of the option ({@link Integer}, {@link
 *     android.graphics.Bitmap.CompressFormat} etc.), must implement {@link #equals(Object)} and
 *     {@link #hashCode()}.
 */
public final class Option<T> {
  private static final CacheKeyUpdater<Object> EMPTY_UPDATER =
      new CacheKeyUpdater<Object>() {
        @Override
        public void update(
            @NonNull byte[] keyBytes, @NonNull Object value, @NonNull MessageDigest messageDigest) {
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
   * @param key A unique package prefixed {@link String} that identifies this option (must be stable
   *     across builds, so {@link Class#getName()} should <em>not</em> be used).
   */
  @NonNull
  public static <T> Option<T> memory(@NonNull String key) {
    return new Option<>(key, null, Option.<T>emptyUpdater());
  }

  /**
   * Returns a new {@link Option} that does not affect disk cache keys with the given value as the
   * default value.
   *
   * @param key A unique package prefixed {@link String} that identifies this option (must be stable
   *     across builds, so {@link Class#getName()} should <em>not</em> be used).
   */
  @NonNull
  public static <T> Option<T> memory(@NonNull String key, @NonNull T defaultValue) {
    return new Option<>(key, defaultValue, Option.<T>emptyUpdater());
  }

  /**
   * Returns a new {@link Option} that uses the given {@link
   * com.bumptech.glide.load.Option.CacheKeyUpdater} to update disk cache keys.
   *
   * @param key A unique package prefixed {@link String} that identifies this option (must be stable
   *     across builds, so {@link Class#getName()} should <em>not</em> be used).
   */
  @NonNull
  public static <T> Option<T> disk(
      @NonNull String key, @NonNull CacheKeyUpdater<T> cacheKeyUpdater) {
    return new Option<>(key, null, cacheKeyUpdater);
  }

  /**
   * Returns a new {@link Option} that uses the given {@link
   * com.bumptech.glide.load.Option.CacheKeyUpdater} to update disk cache keys and provides the
   * given value as the default value.
   *
   * @param key A unique package prefixed {@link String} that identifies this option (must be stable
   *     across builds, so {@link Class#getName()} should <em>not</em> be used).
   */
  @NonNull
  public static <T> Option<T> disk(
      @NonNull String key, @Nullable T defaultValue, @NonNull CacheKeyUpdater<T> cacheKeyUpdater) {
    return new Option<>(key, defaultValue, cacheKeyUpdater);
  }

  private Option(
      @NonNull String key, @Nullable T defaultValue, @NonNull CacheKeyUpdater<T> cacheKeyUpdater) {
    this.key = Preconditions.checkNotEmpty(key);
    this.defaultValue = defaultValue;
    this.cacheKeyUpdater = Preconditions.checkNotNull(cacheKeyUpdater);
  }

  /** Returns a reasonable default to use if no other value is set, or {@code null}. */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  @Nullable
  public T getDefaultValue() {
    return defaultValue;
  }

  /**
   * Updates the given {@link MessageDigest} used to construct a cache key with the given value
   * using the {@link com.bumptech.glide.load.Option.CacheKeyUpdater} optionally provided in the
   * constructor.
   */
  public void update(@NonNull T value, @NonNull MessageDigest messageDigest) {
    cacheKeyUpdater.update(getKeyBytes(), value, messageDigest);
  }

  @NonNull
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

  @NonNull
  @SuppressWarnings("unchecked")
  private static <T> CacheKeyUpdater<T> emptyUpdater() {
    return (CacheKeyUpdater<T>) EMPTY_UPDATER;
  }

  @Override
  public String toString() {
    return "Option{" + "key='" + key + '\'' + '}';
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
     *
     * <p>If your {@link Option} shouldn't affect the disk cache key, you should not implement this
     * class and use {@link Option#memory(String)} or {@link Option#memory(String, Object)} instead.
     *
     * @param keyBytes The bytes of the {@link String} used as the key for this particular {@link
     *     Option}. Should be added to the {@code messageDigest} using {@link
     *     MessageDigest#update(byte[])} by all implementations if the digest is updated with the
     *     given {@code value} parameter.
     * @param value The value of of this particular option. Typically you should convert the value
     *     to a byte array using some stable mechanism and then call {@link
     *     MessageDigest#update(byte[])} to update the given digest.
     */
    void update(@NonNull byte[] keyBytes, @NonNull T value, @NonNull MessageDigest messageDigest);
  }
}
