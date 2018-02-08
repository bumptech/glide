package com.bumptech.glide.util;

import android.support.v4.util.ArrayMap;
import android.support.v4.util.SimpleArrayMap;

/** An {@link ArrayMap} that caches its hashCode to support efficient lookup. */
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
