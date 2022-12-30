package com.bumptech.glide.load;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.collection.SimpleArrayMap;
import com.bumptech.glide.util.CachedHashCodeArrayMap;
import java.security.MessageDigest;

/** A set of {@link Option Options} to apply to in memory and disk cache keys. */
public final class Options implements Key {
  private final ArrayMap<Option<?>, Object> values = new CachedHashCodeArrayMap<>();

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
    return "Options{" + "values=" + values + '}';
  }

  @SuppressWarnings("unchecked")
  private static <T> void updateDiskCacheKey(
      @NonNull Option<T> option, @NonNull Object value, @NonNull MessageDigest md) {
    option.update((T) value, md);
  }
}
