package com.bumptech.glide.load.engine;

import android.support.v4.util.Pools.Pool;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.util.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * For a given {@link com.bumptech.glide.load.data.DataFetcher} for a given data class, attempts to
 * fetch the data and then run it through one or more
 * {@link com.bumptech.glide.load.engine.DecodePath}s.
 *
 * @param <Data>         The type of data that will be fetched.
 * @param <ResourceType> The type of intermediate resource that will be decoded within one of the
 *                       {@link com.bumptech.glide.load.engine.DecodePath}s.
 * @param <Transcode>    The type of resource that will be returned as the result if the load and
 *                       one of the decode paths succeeds.
 */
public class LoadPath<Data, ResourceType, Transcode> {
  private final Class<Data> dataClass;
  private final Pool<List<Exception>> listPool;
  private final List<? extends DecodePath<Data, ResourceType, Transcode>> decodePaths;
  private final String failureMessage;

  public LoadPath(Class<Data> dataClass, Class<ResourceType> resourceClass,
      Class<Transcode> transcodeClass,
      List<DecodePath<Data, ResourceType, Transcode>> decodePaths, Pool<List<Exception>> listPool) {
    this.dataClass = dataClass;
    this.listPool = listPool;
    this.decodePaths = Preconditions.checkNotEmpty(decodePaths);
    failureMessage = "Failed LoadPath{" + dataClass.getSimpleName() + "->"
        + resourceClass.getSimpleName() + "->" + transcodeClass.getSimpleName() + "}";
  }

  public Resource<Transcode> load(DataRewinder<Data> rewinder, Options options, int width,
      int height, DecodePath.DecodeCallback<ResourceType> decodeCallback) throws GlideException {
    List<Exception> exceptions = listPool.acquire();
    try {
      return loadWithExceptionList(rewinder, options, width, height, decodeCallback, exceptions);
    } finally {
      listPool.release(exceptions);
    }
  }

  private Resource<Transcode> loadWithExceptionList(DataRewinder<Data> rewinder, Options options,
      int width, int height, DecodePath.DecodeCallback<ResourceType> decodeCallback,
      List<Exception> exceptions) throws GlideException {
    int size = decodePaths.size();
    Resource<Transcode> result = null;
    for (int i = 0; i < size; i++) {
      DecodePath<Data, ResourceType, Transcode> path = decodePaths.get(i);
      try {
        result = path.decode(rewinder, width, height, options, decodeCallback);
      } catch (GlideException e) {
        exceptions.add(e);
      }
      if (result != null) {
        break;
      }
    }

    if (result == null) {
      throw new GlideException(failureMessage, new ArrayList<>(exceptions));
    }

    return result;
  }

  public Class<Data> getDataClass() {
    return dataClass;
  }

  @Override
  public String toString() {
    return "LoadPath{" + "decodePaths="
        + Arrays.toString(decodePaths.toArray(new DecodePath[decodePaths.size()])) + '}';
  }
}
