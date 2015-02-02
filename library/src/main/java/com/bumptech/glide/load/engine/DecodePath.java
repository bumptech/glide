package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DecodePath<DataType, ResourceType, Transcode> {

    interface DecodeCallback<ResourceType> {
        Resource<ResourceType> onResourceDecoded(Resource<ResourceType> resource);
    }

    private final Class<DataType> dataClass;
    private final Class<ResourceType> resourceClass;
    private final Class<Transcode> transcodeClass;
    private final List<? extends ResourceDecoder<DataType, ResourceType>> decoders;
    private final ResourceTranscoder<ResourceType, Transcode> transcoder;

    public DecodePath(Class<DataType> dataClass, Class<ResourceType> resourceClass, Class<Transcode> transcodeClass,
            List<? extends ResourceDecoder<DataType, ResourceType>> decoders,
            ResourceTranscoder<ResourceType, Transcode> transcoder) {
        this.dataClass = dataClass;
        this.resourceClass = resourceClass;
        this.transcodeClass = transcodeClass;
        this.decoders = decoders;
        this.transcoder = transcoder;
    }

    public Resource<Transcode> decode(DataRewinder<DataType> rewinder, int width, int height,
            Map<String, Object> options, DecodeCallback<ResourceType> callback) throws IOException {
        Resource<ResourceType> resource = decodeResource(rewinder, width, height, options);
        if (resource == null) {
            return null;
        } else {
            resource = callback.onResourceDecoded(resource);
            return transcoder.transcode(resource);
        }
    }

    private Resource<ResourceType> decodeResource(DataRewinder<DataType> rewinder, int width, int height,
            Map<String, Object> options) throws IOException {
        Resource<ResourceType> result = null;
        for (ResourceDecoder<DataType, ResourceType> decoder : decoders) {
            DataType data = rewinder.rewindAndGet();
            if (decoder.handles(data)) {
                data = rewinder.rewindAndGet();
                result = decoder.decode(data, width, height, options);
                if (result != null) {
                    break;
                }
            }
        }
        return result;
    }

    public String getDebugString() {
        return "[" + dataClass + "->" + resourceClass + "->" + transcodeClass + "]";
    }

    @Override
    public String toString() {
        return "DecodePath{"
                + " dataClass=" + dataClass
                + ", decoders=" + decoders
                + ", transcoder=" + transcoder
                + '}';
    }
}
