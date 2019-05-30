package com.bumptech.glide.load.model;

import androidx.annotation.Nullable;

/**
 * An interface for lazily creating headers that allows expensive to calculate headers (oauth for
 * example) to be generated in the background during the first fetch.
 *
 * <p>Implementations should implement equals() and hashcode() .
 */
public interface LazyHeaderFactory {
  /**
   * Returns an http header, or {@code null} if no header could be built.
   *
   * <p>Returning {@code null} or an empty String from this method will result in this particular
   * key/value being excluded from the headers provided in the request. If there are multiple
   * factories or values for a particular key, any non-null values will still be included for that
   * key.
   */
  @Nullable
  String buildHeader();
}
