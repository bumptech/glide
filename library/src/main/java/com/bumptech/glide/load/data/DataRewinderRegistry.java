package com.bumptech.glide.load.data;

import java.util.HashMap;
import java.util.Map;

public class DataRewinderRegistry {
    private final Map<Class, DataRewinder.Factory> rewinders = new HashMap<Class, DataRewinder.Factory>();
    private static final DataRewinder.Factory DEFAULT_FACTORY = new DataRewinder.Factory<Object>() {
        @Override
        public DataRewinder<Object> build(Object data) {
            return new DefaultRewinder(data);
        }

        @Override
        public Class getDataClass() {
            throw new UnsupportedOperationException("Not implemented");
        }
    };

    public synchronized void register(DataRewinder.Factory factory) {
        rewinders.put(factory.getDataClass(), factory);
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> DataRewinder<T> build(T data) {
        DataRewinder.Factory factory = rewinders.get(data.getClass());
        if (factory == null) {
            factory = DEFAULT_FACTORY;
        }
        return factory.build(data);
    }

    private static class DefaultRewinder implements DataRewinder<Object> {
        private Object data;

        public DefaultRewinder(Object data) {
            this.data = data;
        }

        @Override
        public Object rewindAndGet() {
            return data;
        }

        @Override
        public void cleanup() {
            // Do nothing.
        }
    }
}
