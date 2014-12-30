package com.bumptech.glide.request;

import android.util.Log;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.data.DataFetcherSet;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.load.data.DataRewinderRegistry;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.model.ModelLoaderRegistry;
import com.bumptech.glide.provider.EncoderRegistry;
import com.bumptech.glide.provider.ResourceDecoderRegistry;
import com.bumptech.glide.provider.ResourceEncoderRegistry;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class RequestContext {
    private static final String TAG = "RequestContext";

    private final ModelLoaderRegistry modelLoaderRegistry;
    private final EncoderRegistry encoderRegistry;
    private final ResourceDecoderRegistry decoderRegistry;
    private final ResourceEncoderRegistry resultEncoderRegistry;
    private DataRewinderRegistry dataRewinderRegistry;

    public RequestContext(ModelLoaderRegistry modelLoaderRegistry, EncoderRegistry encoderRegistry,
            ResourceDecoderRegistry decoderRegistry, ResourceEncoderRegistry resultEncoderRegistry,
            DataRewinderRegistry dataRewinderRegistry) {
        this.modelLoaderRegistry = modelLoaderRegistry;
        this.encoderRegistry = encoderRegistry;
        this.decoderRegistry = decoderRegistry;
        this.resultEncoderRegistry = resultEncoderRegistry;
        this.dataRewinderRegistry = dataRewinderRegistry;
    }

    @SuppressWarnings("unchecked")
    public <X> ResourceEncoder<X> getResultEncoder(Resource<X> resource) throws NoResultEncoderAvailableException {
        ResourceEncoder<X> resourceEncoder = (ResourceEncoder<X>) resultEncoderRegistry.get(resource.get().getClass());
        if (resourceEncoder != null) {
            return resourceEncoder;
        }
        throw new NoResultEncoderAvailableException(resource.get().getClass());
    }


    @SuppressWarnings("unchecked")
    public <X> Encoder<X> getSourceEncoder(X data) throws NoSourceEncoderAvailableException {
        Encoder<X> encoder = encoderRegistry.getEncoder((Class<X>) data.getClass());
        if (encoder != null) {
            return encoder;
        }
        throw new NoSourceEncoderAvailableException(data.getClass());
    }

    public <X> DataRewinder<X> getRewinder(X data) {
        return dataRewinderRegistry.build(data);
    }

    @SuppressWarnings("unchecked")
    public <X, Z> ResourceDecoder<X, Z> getDecoder(DataRewinder<X> rewinder, Class<Z> resourceClass)
            throws NoDecoderAvailableException,
            IOException {
        X data = rewinder.rewindAndGet();
        List<ResourceDecoder<X, Z>> decoders = decoderRegistry.getDecoders((Class<X>) data.getClass(), resourceClass);
        for (ResourceDecoder<X, Z> decoder : decoders) {
            if (decoder.handles(data)) {
                maybeLogFoundDecoder(decoders, decoder);
                return decoder;
            }
            data = rewinder.rewindAndGet();
        }
        throw new NoDecoderAvailableException(data.getClass());
    }

    private static <X, Z> void maybeLogFoundDecoder(List<ResourceDecoder<X, Z>> decoders,
            ResourceDecoder<X, Z> handles) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Found decoder: " + handles + " from " + Arrays.toString(decoders.toArray(new Object[0])));
        }
    }


    public DataFetcherSet getDataFetchers(Object model, int width, int height) {
        return modelLoaderRegistry.getDataFetchers(model, width, height);
    }

    public static class NoResultEncoderAvailableException extends Exception {
        public NoResultEncoderAvailableException(Class<?> resourceClass) {
            super("Failed to find result encoder for resource class: " + resourceClass);
        }
    }

    public static class NoSourceEncoderAvailableException extends Exception {
        public NoSourceEncoderAvailableException(Class<?> dataClass) {
            super("Failed to find source encoder for data class: " + dataClass);
        }
    }

    public static class NoDecoderAvailableException extends Exception {
        public NoDecoderAvailableException(Class<?> dataClass) {
            super("Failed to find decoder that handles data: " + dataClass);
        }
    }
}
