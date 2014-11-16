package com.bumptech.glide.request;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.data.DataFetcherSet;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.model.ModelLoaderRegistry;
import com.bumptech.glide.provider.EncoderRegistry;
import com.bumptech.glide.provider.ResourceDecoderRegistry;
import com.bumptech.glide.provider.ResourceEncoderRegistry;

public class RequestContext {

    private final ModelLoaderRegistry modelLoaderRegistry;
    private final EncoderRegistry encoderRegistry;
    private final ResourceDecoderRegistry decoderRegistry;
    private final ResourceEncoderRegistry resultEncoderRegistry;

    public RequestContext(ModelLoaderRegistry modelLoaderRegistry, EncoderRegistry encoderRegistry,
            ResourceDecoderRegistry decoderRegistry, ResourceEncoderRegistry resultEncoderRegistry) {
        this.modelLoaderRegistry = modelLoaderRegistry;
        this.encoderRegistry = encoderRegistry;
        this.decoderRegistry = decoderRegistry;
        this.resultEncoderRegistry = resultEncoderRegistry;
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

    @SuppressWarnings("unchecked")
    public <X, Z> ResourceDecoder<X, Z> getDecoder(X data, Class<Z> resourceClass) throws NoDecoderAvailableException {
        for (ResourceDecoder<X, Z> decoder :
                decoderRegistry.getDecoders((Class<X>) data.getClass(), resourceClass)) {

            if (decoder.handles(data)) {
                return decoder;
            }
        }
        throw new NoDecoderAvailableException(data.getClass());
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
