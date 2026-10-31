package com.bumptech.glide.load;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.collection.SimpleArrayMap;
import java.security.MessageDigest;

/**
 * A set of {@link Option Options} to apply to in memory and disk cache keys.
 *
 * <p>An {@link Option} explicitly set to its default value is indistinguishable from an {@link
 * Option} that isn't set at all, so it's ignored by {@link #equals(Object)}, {@link #hashCode()}
 * and {@link #updateDiskCacheKey(MessageDigest)}. The value is still retained so that it can
 * override a non-default value in {@link #putAll(Options)}.
 */
public final class Options implements Key {
  private final ArrayMap<Option<?>, Object> values = new ArrayMap<>();

  public void putAll(@NonNull Options other) {
    values.putAll((SimpleArrayMap<Option<?>, Object>) other.values);
  }

  @NonNull
  public <T> Options set(@NonNull Option<T> option, @NonNull T value) {
    values.put(option, value);
    return this;
  }

  // TODO(b/234614365): Expand usage of this method in BaseRequestOptions so that it's usable for
  // other options.
  public Options remove(@NonNull Option<?> option) {
    values.remove(option);
    return this;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T get(@NonNull Option<T> option) {
    return values.containsKey(option) ? (T) values.get(option) : option.getDefaultValue();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Options) {
      Options other = (Options) o;
      return containsNonDefaultValuesOf(other) && other.containsNonDefaultValuesOf(this);
    }
    return false;
  }

  private boolean containsNonDefaultValuesOf(@NonNull Options other) {
    for (int i = 0; i < other.values.size(); i++) {
      Option<?> option = other.values.keyAt(i);
      Object value = other.values.valueAt(i);
      if (!isDefaultValue(option, value) && !value.equals(get(option))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 0;
    for (int i = 0; i < values.size(); i++) {
      Option<?> option = values.keyAt(i);
      Object value = values.valueAt(i);
      if (!isDefaultValue(option, value)) {
        hashCode += option.hashCode() ^ value.hashCode();
      }
    }
    return hashCode;
  }

  @Override
  public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    for (int i = 0; i < values.size(); i++) {
      Option<?> key = values.keyAt(i);
      Object value = values.valueAt(i);
      if (!isDefaultValue(key, value)) {
        updateDiskCacheKey(key, value, messageDigest);
      }
    }
  }

  @Override
  public String toString() {
    return "Options{" + "values=" + values + '}';
  }

  private static boolean isDefaultValue(@NonNull Option<?> option, @NonNull Object value) {
    return value.equals(option.getDefaultValue());
  }

  @SuppressWarnings("unchecked")
  private static <T> void updateDiskCacheKey(
      @NonNull Option<T> option, @NonNull Object value, @NonNull MessageDigest md) {
    option.update((T) value, md);
  }
}
