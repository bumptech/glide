package com.bumptech.glide.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.util.Synthetic;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains an ordered list of {@link ResourceEncoder}s capable of encoding arbitrary resource
 * types.
 */
public class ResourceEncoderRegistry {
  // TODO: this should probably be a put.
  private final List<Entry<?>> encoders = new ArrayList<>();

  public synchronized <Z> void append(
      @NonNull Class<Z> resourceClass, @NonNull ResourceEncoder<Z> encoder) {
    encoders.add(new Entry<>(resourceClass, encoder));
  }

  public synchronized <Z> void prepend(
      @NonNull Class<Z> resourceClass, @NonNull ResourceEncoder<Z> encoder) {
    encoders.add(0, new Entry<>(resourceClass, encoder));
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public synchronized <Z> ResourceEncoder<Z> get(@NonNull Class<Z> resourceClass) {
    //noinspection ForLoopReplaceableByForEach to improve perf
    for (int i = 0, size = encoders.size(); i < size; i++) {
      Entry<?> entry = encoders.get(i);
      if (entry.handles(resourceClass)) {
        return (ResourceEncoder<Z>) entry.encoder;
      }
    }
    // TODO: throw an exception here?
    return null;
  }

  private static final class Entry<T> {
    private final Class<T> resourceClass;
    @Synthetic final ResourceEncoder<T> encoder;

    Entry(@NonNull Class<T> resourceClass, @NonNull ResourceEncoder<T> encoder) {
      this.resourceClass = resourceClass;
      this.encoder = encoder;
    }

    @Synthetic
    boolean handles(@NonNull Class<?> resourceClass) {
      return this.resourceClass.isAssignableFrom(resourceClass);
    }
  }
}
