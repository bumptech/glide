package com.bumptech.glide.provider;

import com.bumptech.glide.util.MultiClassKey;

import java.util.HashMap;
import java.util.Map;

/**
 * A class that allows {@link com.bumptech.glide.provider.DataLoadProvider}s to be registered and retrieved by the
 * data and resource classes they provide encoders and decoders for.
 */
public class DataLoadProviderRegistry {
    private static final MultiClassKey GET_KEY = new MultiClassKey();

    private final Map<MultiClassKey, DataLoadProvider<?, ?>> providers =
            new HashMap<MultiClassKey, DataLoadProvider<?, ?>>();

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
    public <T, Z> void register(Class<T> dataClass, Class<Z> resourceClass, DataLoadProvider<T, Z> provider) {
        //TODO: maybe something like DataLoadProvider<? super T, ? extends Z> may work here
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
        DataLoadProvider<?, ?> result;
        synchronized (GET_KEY) {
            GET_KEY.set(dataClass, resourceClass);
            result = providers.get(GET_KEY);
        }
        if (result == null) {
            result = EmptyDataLoadProvider.get();
        }
        return (DataLoadProvider<T, Z>) result;
    }
}
