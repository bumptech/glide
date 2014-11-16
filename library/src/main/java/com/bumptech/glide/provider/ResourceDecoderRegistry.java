package com.bumptech.glide.provider;

import com.bumptech.glide.load.ResourceDecoder;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("rawtypes")
public class ResourceDecoderRegistry {
    private final List<Entry<?, ?>> decoders = new ArrayList<Entry<?, ?>>();

    @SuppressWarnings("unchecked")
    public synchronized <T, R> List<ResourceDecoder<T, R>> getDecoders(Class<T> dataClass, Class<R> resourceClass) {
        List<ResourceDecoder<T, R>> result = new ArrayList<ResourceDecoder<T, R>>();
        for (Entry entry : decoders) {
            if (entry.handles(dataClass, resourceClass)) {
                result.add(entry.decoder);
            }
        }
        // TODO: cache result list.

        return result;
    }

    public synchronized <T, R> void append(ResourceDecoder<T, R> decoder, Class<T> dataClass, Class<R> resourceClass) {
        decoders.add(new Entry<T, R>(dataClass, resourceClass, decoder));
    }

    private static class Entry<T, R> {
        private final Class<T> dataClass;
        private final Class<R> resourceClass;
        private final ResourceDecoder<T, R> decoder;

        public Entry(Class<T> dataClass, Class<R> resourceClass, ResourceDecoder<T, R> decoder) {
            this.dataClass = dataClass;
            this.resourceClass = resourceClass;
            this.decoder = decoder;
        }

        public boolean handles(Class<?> dataClass, Class<?> resourceClass) {
            return this.dataClass.isAssignableFrom(dataClass) && resourceClass.isAssignableFrom(this.resourceClass);
        }
    }
}
