package com.bumptech.glide.provider;

import com.bumptech.glide.DataLoadProvider;

import java.util.HashMap;
import java.util.Map;

public class DataLoadProviderFactory {
    private static final MultiClassKey GET_KEY = new MultiClassKey();

    private static class MultiClassKey {
        private Class dataClass;
        private Class resourceClass;

        public MultiClassKey() { }

        public MultiClassKey(Class dataClass, Class resourceClass) {
            this.dataClass = dataClass;
            this.resourceClass = resourceClass;
        }

        @Override
        public String toString() {
            return "MultiClassKey{" +
                    "dataClass=" + dataClass +
                    ", resourceClass=" + resourceClass +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            MultiClassKey that = (MultiClassKey) o;

            if (!dataClass.equals(that.dataClass)) {
                return false;
            }
            if (!resourceClass.equals(that.resourceClass)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = dataClass.hashCode();
            result = 31 * result + resourceClass.hashCode();
            return result;
        }

        public void set(Class dataClass, Class resourceClass) {
            this.dataClass = dataClass;
            this.resourceClass = resourceClass;
        }
    }

    private final Map<MultiClassKey, DataLoadProvider> providers = new HashMap<MultiClassKey, DataLoadProvider>();

    public <T, Z> void register(Class<T> dataClass, Class<Z> resourceClass, DataLoadProvider provider) {
        providers.put(new MultiClassKey(dataClass, resourceClass), provider);
    }

    @SuppressWarnings("unchecked")
    public <T, Z> DataLoadProvider<T, Z> get(Class<T> dataClass, Class<Z> resourceClass) {
        GET_KEY.set(dataClass, resourceClass);
        DataLoadProvider<T, Z> result = providers.get(GET_KEY);
        if (result == null) {
            result = EmptyDataLoadProvider.get();
        }
        return result;
    }
}
