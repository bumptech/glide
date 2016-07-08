package com.bumptech.glide.provider;

import android.support.annotation.Nullable;
import com.bumptech.glide.load.ResourceEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains an unordered list of {@link ResourceEncoder}s capable of encoding arbitrary resource
 * types.
 */
public class ResourceEncoderRegistry {
  // TODO: this should probably be a put.
  final List<Entry<?>> encoders = new ArrayList<>();

  public synchronized <Z> void add(Class<Z> resourceClass, ResourceEncoder<Z> encoder) {
    encoders.add(new Entry<>(resourceClass, encoder));
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public synchronized <Z> ResourceEncoder<Z> get(Class<Z> resourceClass) {
    int size = encoders.size();
    for (int i = 0; i < size; i++) {
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
    private final ResourceEncoder<T> encoder;

    Entry(Class<T> resourceClass, ResourceEncoder<T> encoder) {
      this.resourceClass = resourceClass;
      this.encoder = encoder;
    }

    private boolean handles(Class<?> resourceClass) {
      return this.resourceClass.isAssignableFrom(resourceClass);
    }
  }
}
