package com.bumptech.glide.load.engine;

import android.support.v4.util.Pools.Pool;
import android.util.Log;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Attempts to decode and transcode  resource type from a given data type.
 *
 * @param <DataType>     The type of data ResourceType that will be decoded from.
 * @param <ResourceType> The type of intermediate resource that will be decoded.
 * @param <Transcode>    The final type of resource that will be transcoded from ResourceType and
 *                       returned to the caller.
 */
public class DecodePath<DataType, ResourceType, Transcode> {
  private static final String TAG = "DecodePath";
  private final Class<DataType> dataClass;
  private final List<? extends ResourceDecoder<DataType, ResourceType>> decoders;
  private final ResourceTranscoder<ResourceType, Transcode> transcoder;
  private final Pool<List<Exception>> listPool;
  private final String failureMessage;

  public DecodePath(Class<DataType> dataClass, Class<ResourceType> resourceClass,
      Class<Transcode> transcodeClass,
      List<? extends ResourceDecoder<DataType, ResourceType>> decoders,
      ResourceTranscoder<ResourceType, Transcode> transcoder, Pool<List<Exception>> listPool) {
    this.dataClass = dataClass;
    this.decoders = decoders;
    this.transcoder = transcoder;
    this.listPool = listPool;
    failureMessage = "Failed DecodePath{" + dataClass.getSimpleName() + "->"
        + resourceClass.getSimpleName() + "->" + transcodeClass.getSimpleName() + "}";
  }

  public Resource<Transcode> decode(DataRewinder<DataType> rewinder, int width, int height,
      Options options, DecodeCallback<ResourceType> callback) throws GlideException {
    Resource<ResourceType> decoded = decodeResource(rewinder, width, height, options);
    Resource<ResourceType> transformed = callback.onResourceDecoded(decoded);
    return transcoder.transcode(transformed);
  }

  private Resource<ResourceType> decodeResource(DataRewinder<DataType> rewinder, int width,
      int height, Options options) throws GlideException {
    List<Exception> exceptions = listPool.acquire();
    try {
      return decodeResourceWithList(rewinder, width, height, options, exceptions);
    } finally {
      listPool.release(exceptions);
    }
  }

  private Resource<ResourceType> decodeResourceWithList(DataRewinder<DataType> rewinder, int width,
      int height, Options options, List<Exception> exceptions) throws GlideException {
    Resource<ResourceType> result = null;
    for (int i = 0, size = decoders.size(); i < size; i++) {
      ResourceDecoder<DataType, ResourceType> decoder = decoders.get(i);
      try {
        DataType data = rewinder.rewindAndGet();
        if (decoder.handles(data, options)) {
          data = rewinder.rewindAndGet();
          result = decoder.decode(data, width, height, options);
        }
      } catch (IOException e) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
          Log.v(TAG, "Failed to decode data for " + decoder, e);
        }
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

  @Override
  public String toString() {
    return "DecodePath{" + " dataClass=" + dataClass + ", decoders=" + decoders + ", transcoder="
        + transcoder + '}';
  }

  interface DecodeCallback<ResourceType> {
    Resource<ResourceType> onResourceDecoded(Resource<ResourceType> resource);
  }
}
