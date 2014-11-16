package com.bumptech.glide.provider;

import com.bumptech.glide.load.Encoder;

import java.util.ArrayList;
import java.util.List;

public class EncoderRegistry {
    private final List<Entry<?>> encoders = new ArrayList<Entry<?>>();

    @SuppressWarnings("unchecked")
    public synchronized <T> Encoder<T> getEncoder(Class<T> dataClass) {
        for (Entry<?> entry : encoders) {
            if (entry.handles(dataClass)) {
                return (Encoder<T>) entry.encoder;
            }
        }
        // TODO: throw an exception here.
        return null;
    }

    public synchronized <T> void add(Class<T> dataClass, Encoder<T> encoder) {
        encoders.add(new Entry<T>(dataClass, encoder));
    }

    private static class Entry<T> {
        private final Class<T> dataClass;
        private final Encoder<T> encoder;

        public Entry(Class<T> dataClass, Encoder<T> encoder) {
            this.dataClass = dataClass;
            this.encoder = encoder;
        }

        public boolean handles(Class<?> dataClass) {
            return this.dataClass.isAssignableFrom(dataClass);
        }
    }
}
