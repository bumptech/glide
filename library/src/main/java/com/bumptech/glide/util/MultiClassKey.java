package com.bumptech.glide.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** A key of two {@link Class}es to be used in hashed collections. */
@SuppressWarnings({"PMD.ConstructorCallsOverridableMethod"})
public class MultiClassKey {
  private Class<?> first;
  private Class<?> second;
  private Class<?> third;

  public MultiClassKey() {
    // leave them null
  }

  public MultiClassKey(@NonNull Class<?> first, @NonNull Class<?> second) {
    set(first, second);
  }

  public MultiClassKey(
      @NonNull Class<?> first, @NonNull Class<?> second, @Nullable Class<?> third) {
    set(first, second, third);
  }

  public void set(@NonNull Class<?> first, @NonNull Class<?> second) {
    set(first, second, null);
  }

  public void set(@NonNull Class<?> first, @NonNull Class<?> second, @Nullable Class<?> third) {
    this.first = first;
    this.second = second;
    this.third = third;
  }

  @Override
  public String toString() {
    return "MultiClassKey{" + "first=" + first + ", second=" + second + '}';
  }

  @SuppressWarnings({"PMD.SimplifyBooleanReturns", "RedundantIfStatement"})
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MultiClassKey that = (MultiClassKey) o;

    if (!first.equals(that.first)) {
      return false;
    }
    if (!second.equals(that.second)) {
      return false;
    }
    if (!Util.bothNullOrEqual(third, that.third)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = first.hashCode();
    result = 31 * result + second.hashCode();
    result = 31 * result + (third != null ? third.hashCode() : 0);
    return result;
  }
}
