package com.bumptech.glide.provider;

import android.support.annotation.Nullable;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.util.Synthetic;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains an unordered list of {@link Encoder}s capable of encoding arbitrary data types.
 */
public class EncoderRegistry {
  // TODO: This registry should probably contain a put, rather than a list.
  private final List<Entry<?>> encoders = new ArrayList<>();

  @SuppressWarnings("unchecked")
  @Nullable
  public synchronized <T> Encoder<T> getEncoder(Class<T> dataClass) {
    for (Entry<?> entry : encoders) {
      if (entry.handles(dataClass)) {
        return (Encoder<T>) entry.encoder;
      }
    }
    return null;
  }

  public synchronized <T> void add(Class<T> dataClass, Encoder<T> encoder) {
    encoders.add(new Entry<>(dataClass, encoder));
  }

  private static final class Entry<T> {
    private final Class<T> dataClass;
    @Synthetic final Encoder<T> encoder;

    public Entry(Class<T> dataClass, Encoder<T> encoder) {
      this.dataClass = dataClass;
      this.encoder = encoder;
    }

    public boolean handles(Class<?> dataClass) {
      return this.dataClass.isAssignableFrom(dataClass);
    }
  }
}
