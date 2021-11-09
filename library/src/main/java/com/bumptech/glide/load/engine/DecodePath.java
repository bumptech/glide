package com.bumptech.glide.load.engine;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.util.Pools.Pool;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.util.Preconditions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Attempts to decode and transcode resource type from a given data type.
 *
 * @param <DataType> The type of data ResourceType that will be decoded from.
 * @param <ResourceType> The type of intermediate resource that will be decoded.
 * @param <Transcode> The final type of resource that will be transcoded from ResourceType and
 *     returned to the caller.
 */
public class DecodePath<DataType, ResourceType, Transcode> {
  private static final String TAG = "DecodePath";
  private final Class<DataType> dataClass;
  private final List<? extends ResourceDecoder<DataType, ResourceType>> decoders;
  private final ResourceTranscoder<ResourceType, Transcode> transcoder;
  private final Pool<List<Throwable>> listPool;
  private final String failureMessage;

  public DecodePath(
      Class<DataType> dataClass,
      Class<ResourceType> resourceClass,
      Class<Transcode> transcodeClass,
      List<? extends ResourceDecoder<DataType, ResourceType>> decoders,
      ResourceTranscoder<ResourceType, Transcode> transcoder,
      Pool<List<Throwable>> listPool) {
    this.dataClass = dataClass;
    this.decoders = decoders;
    this.transcoder = transcoder;
    this.listPool = listPool;
    failureMessage =
        "Failed DecodePath{"
            + dataClass.getSimpleName()
            + "->"
            + resourceClass.getSimpleName()
            + "->"
            + transcodeClass.getSimpleName()
            + "}";
  }
  // TODO: Glide源码-into流程-解析资源
  public Resource<Transcode> decode(
      DataRewinder<DataType> rewinder,
      int width,
      int height,
      @NonNull Options options,
      DecodeCallback<ResourceType> callback)
      throws GlideException {
    //调用 decodeResource 将数据解析成中间资源
    Resource<ResourceType> decoded = decodeResource(rewinder, width, height, options);
    //解析完数据回调出去DecodeJob
    Resource<ResourceType> transformed = callback.onResourceDecoded(decoded);
    //转换资源为目标资源BitmapDrawableTranscoder--最后往上返回到DecodeJob#decodeFromRetrievedData 方法中
    return transcoder.transcode(transformed, options);
  }

  @NonNull
  // TODO: Glide源码-into流程-解析资源
  private Resource<ResourceType> decodeResource(
      DataRewinder<DataType> rewinder, int width, int height, @NonNull Options options)
      throws GlideException {
    List<Throwable> exceptions = Preconditions.checkNotNull(listPool.acquire());
    try {
      return decodeResourceWithList(rewinder, width, height, options, exceptions);
    } finally {
      listPool.release(exceptions);
    }
  }

  @NonNull
  // TODO: Glide源码-into流程-解析资源
  private Resource<ResourceType> decodeResourceWithList(
      DataRewinder<DataType> rewinder,
      int width,
      int height,
      @NonNull Options options,
      List<Throwable> exceptions)
      throws GlideException {
    Resource<ResourceType> result = null;
    //noinspection ForLoopReplaceableByForEach to improve perf
    for (int i = 0, size = decoders.size(); i < size; i++) {
      ResourceDecoder<DataType, ResourceType> decoder = decoders.get(i);
      try {
        DataType data = rewinder.rewindAndGet();
        if (decoder.handles(data, options)) {
          data = rewinder.rewindAndGet();
          //如果从磁盘缓存调用
          // 调用 StreamBitmapDecoder.decode 解析数据 StreamBitmapDecoder
          result = decoder.decode(data, width, height, options);
        }
        // Some decoders throw unexpectedly. If they do, we shouldn't fail the entire load path, but
        // instead log and continue. See #2406 for an example.
      } catch (IOException | RuntimeException | OutOfMemoryError e) {
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
    return "DecodePath{"
        + " dataClass="
        + dataClass
        + ", decoders="
        + decoders
        + ", transcoder="
        + transcoder
        + '}';
  }

  interface DecodeCallback<ResourceType> {
    @NonNull
    Resource<ResourceType> onResourceDecoded(@NonNull Resource<ResourceType> resource);
  }
}
