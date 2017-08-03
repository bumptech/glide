package com.bumptech.glide.provider;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.util.Synthetic;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains an ordered list of {@link ResourceDecoder}s capable of decoding arbitrary data types
 * into arbitrary resource types from highest priority decoders to lowest priority decoders.
 */
@SuppressWarnings("rawtypes")
public class ResourceDecoderRegistry {
  private final List<Entry<?, ?>> decoders = new ArrayList<>();

  @SuppressWarnings("unchecked")
  public synchronized <T, R> List<ResourceDecoder<T, R>> getDecoders(Class<T> dataClass,
      Class<R> resourceClass) {
    List<ResourceDecoder<T, R>> result = new ArrayList<>();
    for (Entry<?, ?> entry : decoders) {
      if (entry.handles(dataClass, resourceClass)) {
        result.add((ResourceDecoder<T, R>) entry.decoder);
      }
    }
    // TODO: cache result list.

    return result;
  }

  @SuppressWarnings("unchecked")
  public synchronized <T, R> List<Class<R>> getResourceClasses(Class<T> dataClass,
      Class<R> resourceClass) {
    List<Class<R>> result = new ArrayList<>();
    for (Entry<?, ?> entry : decoders) {
      if (entry.handles(dataClass, resourceClass)) {
        result.add((Class<R>) entry.resourceClass);
      }
    }
    return result;
  }

  public synchronized <T, R> void append(ResourceDecoder<T, R> decoder, Class<T> dataClass,
      Class<R> resourceClass) {
    decoders.add(new Entry<>(dataClass, resourceClass, decoder));
  }

  public synchronized <T, R> void prepend(ResourceDecoder<T, R> decoder, Class<T> dataClass,
      Class<R> resourceClass) {
    decoders.add(0, new Entry<>(dataClass, resourceClass, decoder));
  }

  private static class Entry<T, R> {
    private final Class<T> dataClass;
    @Synthetic final Class<R> resourceClass;
    @Synthetic final ResourceDecoder<T, R> decoder;

    public Entry(Class<T> dataClass, Class<R> resourceClass, ResourceDecoder<T, R> decoder) {
      this.dataClass = dataClass;
      this.resourceClass = resourceClass;
      this.decoder = decoder;
    }

    public boolean handles(Class<?> dataClass, Class<?> resourceClass) {
      return this.dataClass.isAssignableFrom(dataClass) && resourceClass
          .isAssignableFrom(this.resourceClass);
    }
  }
}
