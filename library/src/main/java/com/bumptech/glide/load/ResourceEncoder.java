package com.bumptech.glide.load;

import com.bumptech.glide.load.engine.Resource;

import java.util.Map;

/**
 * An interface for writing data from a resource to some persistent data store (i.e. a local File
 * cache).
 *
 * @param <T> The type of the data contained by the resource.
 */
public interface ResourceEncoder<T> extends Encoder<Resource<T>> {
  // specializing the generic arguments
  EncodeStrategy getEncodeStrategy(Map<String, Object> options);
}
