package com.bumptech.glide.provider;

import java.util.HashMap;
import java.util.Map;

/**
 * A class that allows {@link com.bumptech.glide.provider.DataLoadProvider}s to be registered and retrieved by the
 * data and resource classes they provide encoders and decoders for.
 */
public class DataLoadProviderRegistry {
    private static final MultiClassKey GET_KEY = new MultiClassKey();

    private final Map<MultiClassKey, DataLoadProvider> providers = new HashMap<MultiClassKey, DataLoadProvider>();

    /**
     * Registers the given {@link com.bumptech.glide.provider.DataLoadProvider} using the given classes so it can later
     * be retrieved using the given classes.
     *
     * @param dataClass The class of the data that the provider provides encoders and decoders for.
     * @param resourceClass The class of the resource that the provider provides encoders and decoders for.
     * @param provider The provider.
     * @param <T> The type of the data that the provider provides encoders and decoders for.
     * @param <Z> The type of the resource that the provider provides encoders and decoders for.
     */
    public <T, Z> void register(Class<T> dataClass, Class<Z> resourceClass, DataLoadProvider provider) {
        providers.put(new MultiClassKey(dataClass, resourceClass), provider);
    }

    /**
     * Returns the currently registered {@link com.bumptech.glide.provider.DataLoadProvider} for the given classes.
     *
     * @param dataClass The class of the data that the provider provides encoders and decoders for.
     * @param resourceClass The class of the resource that the provider provides encoders and decoders for.
     * @param <T> The type of the data that the provider provides encoders and decoders for.
     * @param <Z> The type of the resource that the provider provides encoders and decoders for.
     */
    @SuppressWarnings("unchecked")
    public <T, Z> DataLoadProvider<T, Z> get(Class<T> dataClass, Class<Z> resourceClass) {
        GET_KEY.set(dataClass, resourceClass);
        DataLoadProvider<T, Z> result = providers.get(GET_KEY);
        if (result == null) {
            result = EmptyDataLoadProvider.get();
        }
        return result;
    }

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
            return "MultiClassKey{"
                    + "dataClass=" + dataClass
                    + ", resourceClass=" + resourceClass
                    + '}';
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
}
