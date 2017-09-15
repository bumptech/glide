package com.bumptech.glide.provider;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.util.Synthetic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains an ordered list of {@link ResourceDecoder}s capable of decoding arbitrary data types
 * into arbitrary resource types from highest priority decoders to lowest priority decoders.
 */
@SuppressWarnings("rawtypes")
public class ResourceDecoderRegistry {
  private final List<String> bucketPriorityList = new ArrayList<>();
  private final Map<String, List<Entry<?, ?>>> decoders = new HashMap<>();

  public synchronized void setBucketPriorityList(List<String> buckets) {
    List<String> previousBuckets = new ArrayList<>(bucketPriorityList);
    bucketPriorityList.clear();
    bucketPriorityList.addAll(buckets);
    for (String previousBucket : previousBuckets) {
      if (!buckets.contains(previousBucket)) {
        // Keep any buckets from the previous list that aren't included here, but but them at the
        // end.
        bucketPriorityList.add(previousBucket);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public synchronized <T, R> List<ResourceDecoder<T, R>> getDecoders(Class<T> dataClass,
      Class<R> resourceClass) {
    List<ResourceDecoder<T, R>> result = new ArrayList<>();
    for (String bucket : bucketPriorityList) {
      List<Entry<?, ?>> entries = decoders.get(bucket);
      if (entries == null) {
        continue;
      }
      for (Entry<?, ?> entry : entries) {
        if (entry.handles(dataClass, resourceClass)) {
          result.add((ResourceDecoder<T, R>) entry.decoder);
        }
      }
    }
    // TODO: cache result list.

    return result;
  }

  @SuppressWarnings("unchecked")
  public synchronized <T, R> List<Class<R>> getResourceClasses(Class<T> dataClass,
      Class<R> resourceClass) {
    List<Class<R>> result = new ArrayList<>();
    for (String bucket : bucketPriorityList) {
      List<Entry<?, ?>> entries = decoders.get(bucket);
      if (entries == null) {
        continue;
      }
      for (Entry<?, ?> entry : entries) {
        if (entry.handles(dataClass, resourceClass)) {
          result.add((Class<R>) entry.resourceClass);
        }
      }
    }
    return result;
  }

  public synchronized <T, R> void append(
      String bucket, ResourceDecoder<T, R> decoder, Class<T> dataClass, Class<R> resourceClass) {
    getOrAddEntryList(bucket).add(new Entry<>(dataClass, resourceClass, decoder));
  }

  public synchronized <T, R> void prepend(
      String bucket, ResourceDecoder<T, R> decoder, Class<T> dataClass, Class<R> resourceClass) {
    getOrAddEntryList(bucket).add(0, new Entry<>(dataClass, resourceClass, decoder));
  }

  private synchronized List<Entry<?, ?>> getOrAddEntryList(String bucket) {
    if (!bucketPriorityList.contains(bucket)) {
      // Add this unspecified bucket as a low priority bucket.
      bucketPriorityList.add(bucket);
    }
    List<Entry<?, ?>> entries = decoders.get(bucket);
    if (entries == null) {
      entries = new ArrayList<>();
      decoders.put(bucket, entries);
    }
    return entries;
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
