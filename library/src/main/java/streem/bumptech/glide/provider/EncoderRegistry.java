package com.bumptech.glide.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.util.Synthetic;
import java.util.ArrayList;
import java.util.List;

/** Contains an ordered list of {@link Encoder}s capable of encoding arbitrary data types. */
public class EncoderRegistry {
  // TODO: This registry should probably contain a put, rather than a list.
  private final List<Entry<?>> encoders = new ArrayList<>();

  @SuppressWarnings("unchecked")
  @Nullable
  public synchronized <T> Encoder<T> getEncoder(@NonNull Class<T> dataClass) {
    for (Entry<?> entry : encoders) {
      if (entry.handles(dataClass)) {
        return (Encoder<T>) entry.encoder;
      }
    }
    return null;
  }

  public synchronized <T> void append(@NonNull Class<T> dataClass, @NonNull Encoder<T> encoder) {
    encoders.add(new Entry<>(dataClass, encoder));
  }

  public synchronized <T> void prepend(@NonNull Class<T> dataClass, @NonNull Encoder<T> encoder) {
    encoders.add(0, new Entry<>(dataClass, encoder));
  }

  private static final class Entry<T> {
    private final Class<T> dataClass;

    @Synthetic
    @SuppressWarnings("WeakerAccess")
    final Encoder<T> encoder;

    Entry(@NonNull Class<T> dataClass, @NonNull Encoder<T> encoder) {
      this.dataClass = dataClass;
      this.encoder = encoder;
    }

    boolean handles(@NonNull Class<?> dataClass) {
      return this.dataClass.isAssignableFrom(dataClass);
    }
  }
}
