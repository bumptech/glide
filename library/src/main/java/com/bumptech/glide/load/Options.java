package com.bumptech.glide.load;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.SimpleArrayMap;
import java.security.MessageDigest;

/**
 * A set of {@link Option Options} to apply to in memory and disk cache keys.
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

  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T get(@NonNull Option<T> option) {
    return values.containsKey(option) ? (T) values.get(option) : option.getDefaultValue();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Options) {
      Options other = (Options) o;
      return values.equals(other.values);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return values.hashCode();
  }

  @Override
  public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    for (int i = 0; i < values.size(); i++) {
      Option<?> key = values.keyAt(i);
      Object value = values.valueAt(i);
      updateDiskCacheKey(key, value, messageDigest);
    }
  }

  @Override
  public String toString() {
    return "Options{"
        + "values=" + values
        + '}';
  }

  @SuppressWarnings("unchecked")
  private static <T> void updateDiskCacheKey(@NonNull Option<T> option, @NonNull Object value,
      @NonNull MessageDigest md) {
    option.update((T) value, md);
  }
}
