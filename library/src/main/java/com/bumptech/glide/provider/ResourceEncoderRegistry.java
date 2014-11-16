package com.bumptech.glide.provider;

import com.bumptech.glide.load.ResourceEncoder;

import java.util.ArrayList;
import java.util.List;

public class ResourceEncoderRegistry {
    final List<Entry<?>> encoders = new ArrayList<Entry<?>>();

    public synchronized <Z> void add(Class<Z> resourceClass, ResourceEncoder<Z> encoder) {
        encoders.add(new Entry<Z>(resourceClass, encoder));
    }

    @SuppressWarnings("unchecked")
    public synchronized <Z> ResourceEncoder<Z> get(Class<Z> resourceClass) {
        for (Entry<?> entry : encoders) {
            if (entry.handles(resourceClass)) {
                return (ResourceEncoder<Z>) entry.encoder;
            }
        }
        // TODO: throw an exception here?
        return null;
    }

    private static class Entry<T> {
        private final Class<T> resourceClass;
        private final ResourceEncoder<T> encoder;

        private Entry(Class<T> resourceClass, ResourceEncoder<T> encoder) {
            this.resourceClass = resourceClass;
            this.encoder = encoder;
        }

        private boolean handles(Class<?> resourceClass) {
            return this.resourceClass.isAssignableFrom(resourceClass);
        }
    }
}
