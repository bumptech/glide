package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.util.Preconditions;

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
  private final List<? extends DecodePath<Data, ResourceType, Transcode>> decodePaths;

  public LoadPath(Class<Data> dataClass,
      List<DecodePath<Data, ResourceType, Transcode>> decodePaths) {
    this.dataClass = dataClass;
    this.decodePaths = Preconditions.checkNotEmpty(decodePaths);
  }

  public Resource<Transcode> load(Data data, RequestContext<?, Transcode> context,
      int width, int height, DecodePath.DecodeCallback<ResourceType> decodeCallback) {
    Preconditions.checkNotNull(data);

    Resource<Transcode> result = null;
    Options options = context.getOptions();
    DataRewinder<Data> rewinder = context.getRewinder(data);
    try {
      int size = decodePaths.size();
      for (int i = 0; i < size; i++) {
        DecodePath<Data, ResourceType, Transcode> path = decodePaths.get(i);
        result = path.decode(rewinder, width, height, options, decodeCallback);
        if (result != null) {
          break;
        }
      }
    } finally {
      rewinder.cleanup();
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
