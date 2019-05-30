package com.bumptech.glide.util;

import androidx.collection.ArrayMap;
import androidx.collection.SimpleArrayMap;

/**
 * An {@link ArrayMap} that caches its hashCode to support efficient lookup.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 */
// We're overriding hashcode, but not in a way that changes the output, so we don't need to
// override equals.
@SuppressWarnings("PMD.OverrideBothEqualsAndHashcode")
public final class CachedHashCodeArrayMap<K, V> extends ArrayMap<K, V> {

  private int hashCode;

  @Override
  public void clear() {
    hashCode = 0;
    super.clear();
  }

  @Override
  public V setValueAt(int index, V value) {
    hashCode = 0;
    return super.setValueAt(index, value);
  }

  @Override
  public V put(K key, V value) {
    hashCode = 0;
    return super.put(key, value);
  }

  @Override
  public void putAll(SimpleArrayMap<? extends K, ? extends V> simpleArrayMap) {
    hashCode = 0;
    super.putAll(simpleArrayMap);
  }

  @Override
  public V removeAt(int index) {
    hashCode = 0;
    return super.removeAt(index);
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      hashCode = super.hashCode();
    }
    return hashCode;
  }
}
