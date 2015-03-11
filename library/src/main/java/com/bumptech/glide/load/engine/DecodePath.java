package com.bumptech.glide.load.engine;

import android.util.Log;

import com.bumptech.glide.Logs;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

import java.io.IOException;
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
  private final Class<DataType> dataClass;
  private final List<? extends ResourceDecoder<DataType, ResourceType>> decoders;
  private final ResourceTranscoder<ResourceType, Transcode> transcoder;

  public DecodePath(Class<DataType> dataClass,
      List<? extends ResourceDecoder<DataType, ResourceType>> decoders,
      ResourceTranscoder<ResourceType, Transcode> transcoder) {
    this.dataClass = dataClass;
    this.decoders = decoders;
    this.transcoder = transcoder;
  }

  public Resource<Transcode> decode(DataRewinder<DataType> rewinder, int width, int height,
      Options options, DecodeCallback<ResourceType> callback) {
    Resource<ResourceType> decoded = decodeResource(rewinder, width, height, options);
    if (decoded == null) {
      return null;
    }
    Resource<ResourceType> transformed = callback.onResourceDecoded(decoded);
    return transcoder.transcode(transformed);
  }

  private Resource<ResourceType> decodeResource(DataRewinder<DataType> rewinder, int width,
      int height, Options options) {
    Resource<ResourceType> result = null;
    for (ResourceDecoder<DataType, ResourceType> decoder : decoders) {
      try {
        DataType data = rewinder.rewindAndGet();
        if (decoder.handles(data, options)) {
          data = rewinder.rewindAndGet();
          result = decoder.decode(data, width, height, options);
        }
      } catch (IOException e) {
        if (Logs.isEnabled(Log.VERBOSE)) {
          Logs.log(Log.VERBOSE, "Failed to decode data for " + decoder, e);
        }
      }

      if (result != null) {
        break;
      }
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
